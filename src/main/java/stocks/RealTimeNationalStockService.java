package stocks;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class RealTimeNationalStockService {

	private final String uriTemplate = "wss://national-stock-service.herokuapp.com/stocks/realtime/%s";

	private Map<String, SubscriberSocket> subscribers = new HashMap<>();

	public RealTimeNationalStockService() {
	}

	public String subscribeTo(String ticker, Consumer<String> onMessage, Consumer<Throwable> onError,
			BiConsumer<Integer, String> onClose) throws MalformedURLException, URISyntaxException {
		final String wssUri = (ticker == null || ticker.isEmpty()) ? String.format(uriTemplate, "") : String.format(uriTemplate, ticker);
		SubscriberSocket subscriberSocket = new SubscriberSocket(new URI(wssUri), onMessage, onError, onClose);
		subscriberSocket.connect();
		System.out.println("Connecting to RealTimeNationalStockService..." + wssUri);
		String uuid = UUID.randomUUID().toString();
		subscribers.put(uuid, subscriberSocket);
		while (subscriberSocket.isConnecting())
			;
		return uuid;
	}

	public void unsubscribe(String clientId) {
		if (subscribers.containsKey(clientId)) {
			final SubscriberSocket subscriberSocket = subscribers.get(clientId);
			if (subscriberSocket.isOpen())
				subscriberSocket.send("unsubscribe");
		}
	}

	public void close(String clientId) {
		if (subscribers.containsKey(clientId)) {
			final SubscriberSocket subscriberSocket = subscribers.get(clientId);
			if (subscriberSocket.isOpen()) {
				subscriberSocket.close();
				subscribers.remove(clientId);
			}
		}
	}

	private static class SubscriberSocket extends WebSocketClient {
		private Consumer<String> onMessage;
		private Consumer<Throwable> onError;
		private BiConsumer<Integer, String> onClose;

		public SubscriberSocket(URI serverUri, Consumer<String> onMessage, Consumer<Throwable> onError,
				BiConsumer<Integer, String> onClose) {
			super(serverUri);
			this.onMessage = onMessage;
			this.onError = onError;
			this.onClose = onClose;
		}

		@Override
		public void onOpen(ServerHandshake handshakedata) {
			System.out.println("Connected");
			send("subscribe");
		}

		@Override
		public void onMessage(String message) {
			onMessage.accept(message);
		}

		@Override
		public void onClose(int code, String reason, boolean remote) {
			onClose.accept(code, reason);
			System.out.println("Disconnected");
		}

		@Override
		public void onError(Exception ex) {
			onError.accept(ex);
		}
	}

	public static void main(String[] args) throws URISyntaxException, MalformedURLException {
		RealTimeNationalStockService stockService = new RealTimeNationalStockService();
		String clientId = stockService.subscribeTo("GOOG", message -> System.out.println(message), System.out::println,
				(code, reason) -> System.out.println("Closed " + code + " " + reason));

		try {
			Thread.sleep(20 * 1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		stockService.unsubscribe(clientId);
		stockService.close(clientId);
	}
}
