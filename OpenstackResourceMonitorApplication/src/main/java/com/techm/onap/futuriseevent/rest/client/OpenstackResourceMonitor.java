package com.techm.onap.futuriseevent.rest.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONObject;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.json.JSONArray;

public class OpenstackResourceMonitor {

	static String resourceId;
	static Date now;
	static OpenstackResourceMonitor monitor = new OpenstackResourceMonitor();
	static HttpURLConnection conn;
	static BufferedReader br;
	static boolean tokenAlive;
	static String token;
	static final String nagiosIPAddress = "10.20.120.70";
	static final String community = "clearwater";
	// Ideally Port 162 should be used to send receive Trap, any other available
	// Port can be used
	public static final int port = 162;
	// IP of ONAP VM
	public static final String onapIPAddress = "10.53.172.115";
	// Sending Trap for sysLocation of RFC1213
	public static final String Oid = "1.3.6.1.4.1.19444.12.2.0.6";

	// CPU Usage of homestead
	public static final String Oid2 = "1.2.826.0.1.1578918.9.3.6.1.3";
	// homestead IP address
	public static final String Oid3 = "1.3.6.1.4.1.19444.12.2.0.1";

	static {

		generateTokenWhenExpired();

	}

	public static void main(String[] args) {

		Timer time = new Timer(); // Instantiate Timer Object

		time.schedule(new TimerTask() {

			@Override
			public void run() {

				System.out.println(
						"\n\n\n============================Extracting ** =============================" + token);

				try {

					if (token != null)
						monitor.checkResourceStatus();

				} catch (MalformedURLException e) {

					e.printStackTrace();
				} catch (IOException e) {

					e.printStackTrace();
				} finally {

					try {
						if (br != null)
							br.close();
						if (conn != null)
							conn.disconnect();
					} catch (Exception ex) {

					}
				}

			}

		}, 0, 5000);
		// Create Repetitively task for every 5 sec

	}

	public static void sendTrapToNagios(String homestead, String nagiosIPAddress, String description) {

		try {
			// Create Transport Mapping
			TransportMapping transport = new DefaultUdpTransportMapping();
			transport.listen();

			System.out.println("-- transport is listening now --");

			// Create Target
			CommunityTarget cTarget = new CommunityTarget();
			cTarget.setCommunity(new OctetString(community));
			cTarget.setVersion(SnmpConstants.version2c);
			cTarget.setAddress(new UdpAddress(nagiosIPAddress + "/" + port));
			cTarget.setRetries(2);
			cTarget.setTimeout(5000);

			System.out.println("-- Community target prepared --");

			// Create PDU for V2
			PDU pdu = new PDU();

			// need to specify the system up time
			pdu.add(new VariableBinding(SnmpConstants.sysUpTime, new OctetString(new Date().toString())));
			pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, new OID(Oid)));
			pdu.add(new VariableBinding(SnmpConstants.snmpTrapAddress, new IpAddress(onapIPAddress)));
			pdu.add(new VariableBinding(new OID(Oid), new OctetString(description)));
			pdu.add(new VariableBinding(new OID(Oid2), new OctetString(homestead)));
			pdu.add(new VariableBinding(new OID(Oid3), new OctetString(nagiosIPAddress)));
			pdu.setType(PDU.NOTIFICATION);

			System.out.println("-- PDU prepared --");

			// Send the PDU
			Snmp snmp = new Snmp(transport);

			System.out.println("-- Sending V2 Trap to NAGIOS now --");

			snmp.send(pdu, cTarget);
			snmp.close();

			System.out.println("-- trap sent --");

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void checkResourceStatus() throws IOException {

		extractResources();

	}

	private void extractResources() throws IOException {

		System.out.println("------extractResources------");

		JSONObject resource = null;

		URL url = new URL("http://10.53.214.138:8041/v1/resource/instance");
		conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setConnectTimeout(5000);
		conn.setReadTimeout(5000);
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("X-AUTH-TOKEN", token);

		if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
			throw new RuntimeException("Failed : Unable to connect to URL ... ");

		}
		System.out.println("Connected to the server to retrieve resource informaiton...");

		br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

		System.out.println("Reading resource data from server...");

		String output;
		StringBuffer response = new StringBuffer();
		// System.out.println("Output from Server .... \n");
		while ((output = br.readLine()) != null) {

			response.append(output);
		}

		JSONArray resources = new JSONArray(response.toString());

		for (int i = 0; i < resources.length(); i++) {

			resource = (JSONObject) resources.get(i);

			System.out.println("Extracting metric details for resource : " + resource.getString("id"));

			//extractMetricDetails(resource.getJSONObject("metrics").getString("cpu.util"));
			extractMetricMeasures(resource.getJSONObject("metrics").getString("cpu.util"));

		}

	}

	private static void extractMetricDetails(String metricId) throws IOException {

		BufferedReader br = null;
		HttpURLConnection conn = null;

		URL url = new URL("http://10.53.214.138:8041/v1/metric/" + metricId);
		conn = (HttpURLConnection) url.openConnection();
		conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setConnectTimeout(5000);
		conn.setReadTimeout(5000);
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("X-AUTH-TOKEN", token);

		if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
			throw new RuntimeException("Failed : Unable to connect to URL ... ");

		}
		System.out.println("Connected to the server to retrive metric cpu.util details...");

		br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

		System.out.println("Reading data from server...");

		String output;
		StringBuffer response = new StringBuffer();
		// System.out.println("Output from Server .... \n");
		while ((output = br.readLine()) != null) {

			response.append(output);
		}

		JSONObject resourceInstance2 = new JSONObject(response.toString());
		JSONObject archivePolicy = resourceInstance2.getJSONObject("archive_policy");
		JSONArray defination = archivePolicy.getJSONArray("definition");
		JSONObject actualMetrics = null;

		for (int i = 0; i < defination.length(); i++) {
			actualMetrics = (JSONObject) defination.get(i);
		}

		System.out.println("points-> " + actualMetrics.getInt("points"));
		System.out.println("timespan-> " + actualMetrics.getString("timespan"));
		System.out.println("granularity-> " + actualMetrics.getString("granularity"));

		conn.disconnect();

	}

	private static void extractMetricMeasures(String metricId) throws IOException {

		BufferedReader br = null;
		HttpURLConnection conn = null;

		URL url = new URL("http://10.53.214.138:8041/v1/metric/" + metricId + "/measures?aggregation=max");
		conn = (HttpURLConnection) url.openConnection();
		conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setConnectTimeout(5000);
		conn.setReadTimeout(5000);
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("X-AUTH-TOKEN", token);

		if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
			throw new RuntimeException("Failed : Unable to connect to URL ... ");

		}
		System.out.println("Connected to the server to retrive metric measures...");

		br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

		System.out.println("Reading data from server...");

		String output;
		StringBuffer response = new StringBuffer();
		// System.out.println("Output from Server .... \n");
		while ((output = br.readLine()) != null) {

			response.append(output);
		}

		JSONArray metricMeasures = new JSONArray(response.toString());
		JSONArray measure = null;
		String cpuUsage = null;

		for (int i = 0; i < metricMeasures.length(); i++) {

			measure = (JSONArray) metricMeasures.get(i);

			for (int j = 0; j < measure.length(); j++) {

				if (j == 2)
					cpuUsage = (String) measure.get(2);

			}

		}

		conn.disconnect();

		sendTrapToNagios(cpuUsage, nagiosIPAddress, "HomeStead having high CPU");

	}

	public static String generateToken() {

		JSONObject jObj = null;
		JSONObject access = null;
		JSONObject token = null;
		HttpURLConnection conn = null;
		BufferedReader br = null;

		try {
			// hitting KeyStone API and get the token details by providing authentication
			URL url = new URL("http://10.53.214.138:35357/v2.0/tokens");
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-Type", "application/json");

			String input = "{\"auth\":{\"passwordCredentials\":{\"username\": \"admin\", \"password\": \"FndtDuHTWzZYxBQ7BgW26PD7z\"}}}";
			OutputStream os = conn.getOutputStream();
			os.write(input.getBytes());
			os.flush();

			br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

			String output;
			StringBuffer response = new StringBuffer();
			System.out.println("\n Token details retrieved from Keystone API .... \n");
			while ((output = br.readLine()) != null) {

				response.append(output);
			}
			jObj = new JSONObject(response.toString());
			access = jObj.getJSONObject("access");
			token = access.getJSONObject("token");
			System.out.println("token-> " + token.getString("id"));

			conn.disconnect();

		} catch (MalformedURLException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		} finally {

			try {
				if (br != null)
					br.close();
				if (conn != null)
					conn.disconnect();
			} catch (Exception ex) {

			}
		}

		return token.getString("id");

	}

	// Assume Token expires every one hour default as per the Openstack
	// documentation
	private static void generateTokenWhenExpired() {

		Timer time = new Timer(); // Instantiate Timer Object
		time.schedule(new TimerTask() {

			@Override
			public void run() {

				token = generateToken();

			}
		}, 0, 3600000);

	}

}
