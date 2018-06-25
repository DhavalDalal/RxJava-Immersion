package stocks;

import java.io.*;
import java.net.*;
import java.security.*;
import javax.net.ssl.*;

import org.json.JSONObject;

public class NationalStockService {
	private final String urlTemplate = "https://national-stock-service.herokuapp.com/stocks/%s";

	public NationalStockService() {
	}

	public double getPrice(final String ticker) throws Exception {
		System.out.println("NationalStockService.getPrice() " + Thread.currentThread());
		setupSSLContext();
		final String httpsUrl = String.format(urlTemplate, ticker);
		final HttpsURLConnection connection = openSSLConnectionTo(httpsUrl);
		final String data = getData(connection);
		final JSONObject stockDetails = new JSONObject(data);
		return stockDetails.getDouble("price");
	}

	private void setupSSLContext() throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext ctx = SSLContext.getInstance("TLS");
		ctx.init(new KeyManager[0], new TrustManager[] { new Empty() }, new SecureRandom());
		SSLContext.setDefault(ctx);
	}

	private JSONObject toJson(final String data) {
		return new JSONObject(data);
	}

	private String getData(final HttpsURLConnection connection) throws IOException {
		InputStream is = connection.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader reader = new BufferedReader(isr);
		String line = null;
		StringBuilder data = new StringBuilder();
		while ((line = reader.readLine()) != null) {
			data.append(line);
		}
		return data.toString();
	}

	private HttpsURLConnection openSSLConnectionTo(String url) throws MalformedURLException, IOException {
		final URL https = new URL(url);
		HttpsURLConnection connection = (HttpsURLConnection) https.openConnection();
		connection.setHostnameVerifier(new VerifiedOK());
		return connection;
	}

	private static class VerifiedOK implements HostnameVerifier {
		@Override
		public boolean verify(String arg0, SSLSession arg1) {
			return true;
		}
	}

	private static class Empty implements X509TrustManager {
		@Override
		public void checkClientTrusted(java.security.cert.X509Certificate[] arg0, String arg1)
				throws java.security.cert.CertificateException {
		}

		@Override
		public void checkServerTrusted(java.security.cert.X509Certificate[] arg0, String arg1)
				throws java.security.cert.CertificateException {
		}

		@Override
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	}
}
