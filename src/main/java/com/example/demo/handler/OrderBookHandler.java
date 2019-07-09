package com.example.demo.handler;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.OrderType;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.NewOrder;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.request.OrderStatusRequest;
import com.binance.api.client.domain.market.TickerPrice;
import com.binance.api.client.domain.market.TickerStatistics;
import com.example.demo.model.OrderBookModel;
import com.example.demo.service.BinanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Component
@org.springframework.core.annotation.Order(2)
public class OrderBookHandler {

    private static final Logger LOG = LoggerFactory.getLogger(OrderBookHandler.class);
    private static final long TEN_SECS = 10000;

    private BlockingQueue<OrderBookModel> queue = new ArrayBlockingQueue<>(1000, true);
    private Map<String, BlockingQueue<OrderBookModel>> queuePerSymbol = new HashMap<>();

    private Map<String, Deque<TickerPrice>> symbolPrices = new HashMap<>(); // contains deque for every symbol(XYZ/BTC)
    private TickerStatistics btcusdtPrice;
    private final BinanceApiRestClient binanceApiRestClient;
    private final BinanceService binanceService;

    @Autowired
    public OrderBookHandler(BinanceApiRestClient binanceApiRestClient,
                            BinanceService binanceService) {
        this.binanceApiRestClient = binanceApiRestClient;
        this.binanceService = binanceService;
    }

    @PostConstruct
    public void init() {
        DataDistributor dataDistributor = new DataDistributor(queue);
        new Thread(dataDistributor).start();
        List<String> symbols = binanceService.getSymbols();
        symbols.forEach(s -> {
            BlockingQueue<OrderBookModel> orderBookModels = new ArrayBlockingQueue<>(1000, true);
            queuePerSymbol.put(s, orderBookModels);
            new Thread(new Handler(orderBookModels)).start();
        });

    }

    /**
     * Runs every minute
     * to calculate the price statistics of BTCUSDT in last 24 hours
     * and price of XYZ/BTC in last 10 minutes
     */
    @Scheduled(fixedDelay=60000) //
    public void getPrice(){
        btcusdtPrice = binanceApiRestClient.get24HrPriceStatistics("BTCUSDT");
        List<TickerPrice> tickerPrices = binanceApiRestClient.getAllPrices();
        for (TickerPrice tickerPrice: tickerPrices){
            String symbol = tickerPrice.getSymbol();
            CharSequence charSequence = symbol.subSequence(symbol.length() - 3, symbol.length());
            if (!charSequence.toString().equals("BTC")) continue;
            Deque<TickerPrice> queue = symbolPrices.get(symbol);
            if (queue == null){
                queue = new LinkedList<>();
                queue.add(tickerPrice);
            }else {
                if (queue.size() == 10){ // 10 records for 10 minutes
                    queue.poll();
                }
                queue.offer(tickerPrice);
            }
            //System.out.println(symbol +" "+queue.size());
            symbolPrices.put(symbol, queue);
        }
    }


    /**
     * Distributes streaming order book data to respective symbol handler
     * Queue is getting filled by controller EventController (/add-event/write)
     * which is executed by the database when something is inserted on it
     */
    public class DataDistributor implements Runnable {
        private BlockingQueue<OrderBookModel> queue;
        DataDistributor(BlockingQueue<OrderBookModel> q) {
            this.queue = q;
        }
        @Override
        public void run() {
            while (true) {
                try {
                    OrderBookModel orderBook = queue.take();
                    BlockingQueue<OrderBookModel> orderBookModels = queuePerSymbol.get(orderBook.getSymbol());
                    orderBookModels.put(orderBook);
                } catch (InterruptedException e) {
                    LOG.error(e.toString());
                }
            }
        }
    }

    /**
     * Handler for each symbol(XYZ/BTC)
     * Queue is getting filled by Distributor for particular symbol
     */
    class Handler implements Runnable{
        private BlockingQueue<OrderBookModel> queue;
        private  Map<Long, Thread> buyers = new HashMap<>();
        private Map<Long, BlockingQueue<OrderBookModel>> buyersVsQueue = new HashMap<>();
        Handler(BlockingQueue<OrderBookModel> q) {
            this.queue = q;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    OrderBookModel orderBook = queue.take();
                    //System.out.println(orderBook);
                    double priceChangePercentBTC = Double.parseDouble(btcusdtPrice.getPriceChangePercent());
                    String symbol = orderBook.getSymbol();
                    int btc = symbol.indexOf("BTC");
                    String coin = symbol.substring(0, btc);
                    double priceChangePercentCoin = 0;
                    try {
                        priceChangePercentCoin = getSymbolPricePercentageChange(coin);
                    } catch (NoPriceFoundException e) {
                       continue;
                    }
                    Set<Long> threadIds = new HashSet<>();
                    for (Map.Entry<Long, Thread> conditionChecker : buyers.entrySet()){
                        Thread thread = conditionChecker.getValue();
                        if (thread.isAlive()){
                            // put the order book in the buyer queue which is checking for the 10 sec window condition
                            BlockingQueue<OrderBookModel> queue = buyersVsQueue.get(thread.getId());
                            queue.put(orderBook);
                        }else {
                            threadIds.add(thread.getId());
                        }
                    }
                    threadIds.forEach(aLong -> { // remove dead thread
                        buyers.remove(aLong);
                        buyersVsQueue.remove(aLong);
                    });
                    System.out.println("Price Change Percent BTC in 24 hrs: "+priceChangePercentBTC + ", Required Price Change Percent BTC in 24 hrs: "+3);
                    if (Math.abs(priceChangePercentBTC) < 3 && Math.abs(priceChangePercentCoin) < 1.5) {
                        LOG.info("Initial condition met");
                        BlockingQueue<OrderBookModel> queue = new ArrayBlockingQueue<>(1000, true);
                        Thread thread = new Thread(new Buyer(queue));
                        buyers.put(thread.getId(), thread);
                        buyersVsQueue.put(thread.getId(), queue); // store the queue with respective buyer thread id
                        queue.put(orderBook);
                        thread.start();
                    }else {
                        //LOG.info("Initial condition not met");
                    }
                } catch (InterruptedException e) {
                    LOG.error(e.toString());
                }
            }
        }

        /**
         * Deque contains 10 records for last 10 minutes
         * @param coin
         * @return percentage change in symbol(XYZ/BTC) in last 10 mins
         * @throws NoPriceFoundException
         */
        private double getSymbolPricePercentageChange(String coin) throws NoPriceFoundException {
            Deque<TickerPrice> tickerStatisticsCoin = symbolPrices.get(coin+"BTC");
            if (tickerStatisticsCoin == null){
                throw new NoPriceFoundException();
            }
            TickerPrice first = tickerStatisticsCoin.getFirst();
            TickerPrice last = tickerStatisticsCoin.getLast();
            return (Double.valueOf(last.getPrice()) -
                    Double.valueOf(first.getPrice())) / Double.valueOf(first.getPrice()) * 100;
        }
    }

    /**
     * Buyer checks for the conditions in 10 sec window after initial conditions are met
     * Queue is filled by the handler for the streaming order book
     */
    class Buyer implements Runnable {
        private BlockingQueue<OrderBookModel> queue;
        Buyer(BlockingQueue<OrderBookModel> q) {
            this.queue = q;
        }

        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            long thresholdTime = currentTime + TEN_SECS; // 10 sec window
            OrderBookModel orderBook = null;
            while (true) {
                try {
                    orderBook = queue.take();
                    LOG.info("Buyer started: {}", orderBook.getSymbol());
                    if (System.currentTimeMillis() > thresholdTime) break;
                    double high = orderBook.getBestBid().getPrice().doubleValue();
                    double low = high * 0.97;
                    Map<BigDecimal, BigDecimal> bids = orderBook.getBids();
                    double totalBitcoin = 0;
                    for (Map.Entry<BigDecimal, BigDecimal> bid : bids.entrySet()) {
                        double price = bid.getKey().doubleValue();
                        if (price >= low && price <= high) {
                            totalBitcoin += price * bid.getValue().doubleValue();
                        }
                    }
                    LOG.info("Total bitcoin bid {}", totalBitcoin);
                    if (totalBitcoin < 3) { // less than equal to 3 BTC
                        break;
                    }
                    double lowAsk = orderBook.getBestAsk().getPrice().doubleValue();
                    double highAsk = lowAsk * 1.03;
                    Map<BigDecimal, BigDecimal> asks = orderBook.getAsks();
                    double totalBitcoinAsk = 0;
                    for (Map.Entry<BigDecimal, BigDecimal> ask : asks.entrySet()) {
                        double price = ask.getKey().doubleValue();
                        if (price >= lowAsk && price <= highAsk) {
                            totalBitcoinAsk += price * ask.getValue().doubleValue();
                        }
                    }
                    LOG.info("Total bitcoin ask {}", totalBitcoinAsk);
                    if (totalBitcoinAsk < 4*totalBitcoin) { // less than 4 times the bid volume
                        break;
                    }
                } catch (InterruptedException e) {
                    LOG.error(e.toString());
                }
            }

            if (System.currentTimeMillis() < thresholdTime) return;
            buy(orderBook); // all conditions satisfied, now buy !!
        }

        private void buy(OrderBookModel orderBookModel) {
            LOG.info("Buying the symbol: {}", orderBookModel.getSymbol());
            String symbol = orderBookModel.getSymbol();
            double buyingPrice = ((orderBookModel.getBestAsk().getPrice().doubleValue() +
                    orderBookModel.getBestBid().getPrice().doubleValue())/2) * 0.005; // 0.5% of avg of best ask and bid

            NewOrderResponse buyOrderResponse = binanceApiRestClient.newOrder( // buy order
                    new NewOrder(
                            symbol,
                            OrderSide.BUY,
                            OrderType.LIMIT,
                            TimeInForce.GTC,
                            "10",
                            String.valueOf(buyingPrice)
                    ));

            LOG.info("BuyOrderResponse: {}", buyOrderResponse);

            NewOrderResponse stopLossOrderResponse = binanceApiRestClient.newOrder( // stop loss
                    new NewOrder(
                            symbol,
                            OrderSide.SELL,
                            OrderType.STOP_LOSS,
                            TimeInForce.GTC,
                            "10",
                            String.valueOf(buyingPrice - 0.07 * buyingPrice)
                    )
            );

            LOG.info("StopLossOrderResponse: {}", stopLossOrderResponse);

            double sellingPrice = buyingPrice + buyingPrice * 0.05;
            NewOrderResponse sellOrderResponse = binanceApiRestClient.newOrder(
                    new NewOrder(
                            symbol,
                            OrderSide.SELL,
                            OrderType.LIMIT,
                            TimeInForce.GTC,
                            "10",
                            String.valueOf(sellingPrice)
                    )
            );

            LOG.info("SellOrderResponse: {}", sellOrderResponse);

            try {
                Thread.sleep(20000); // waiting for 20 secs
            } catch (InterruptedException e) {
                LOG.error(e.toString());
            }

            Order orderStatus = binanceApiRestClient.getOrderStatus(
                    new OrderStatusRequest(sellOrderResponse.getClientOrderId(), sellOrderResponse.getOrderId())
            );

            if (orderStatus.getStatus().equals(OrderStatus.FILLED)) return;

            // Todo :- check do we need to withdraw before placing the sell order again
            NewOrderResponse finalSellOrderResponse = binanceApiRestClient.newOrder( // after 20 secs decrease the sell price by 1%
                    new NewOrder(
                        symbol,
                        OrderSide.SELL,
                        OrderType.LIMIT,
                        TimeInForce.GTC,
                        "10",
                        String.valueOf(sellingPrice - sellingPrice * 0.01)
                    )
            );

            LOG.info("FinalSellOrderResponse: {}", finalSellOrderResponse);
        }
    }

    public BlockingQueue<OrderBookModel> getQueue() {
        return queue;
    }
}
