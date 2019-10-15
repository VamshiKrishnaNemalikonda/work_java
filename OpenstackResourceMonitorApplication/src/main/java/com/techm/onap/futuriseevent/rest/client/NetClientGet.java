package com.techm.onap.futuriseevent.rest.client;

import java.util.Date;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.json.JSONObject;
import java.io.OutputStream;
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

public class NetClientGet {

	public static final String community = "clearwater";

	// Sending Trap for sysLocation of RFC1213
	public static final String Oid = ".1.3.6.1.2.1.1.8";

	// CPU Usage of homestead
	public static final String Oid2 = "1.2.826.0.1.1578918.9.5.6.1.2";

	// homestead IP address
	public static final String Oid3 = ".1.3.6.1.2.1.1.8.2";

	// IP of NAGIOS
	public static final String nagiosIPAddress = "10.20.120.70";

	// IP of ONAP VM
	public static final String onapIPAddress = "10.53.172.115";

	// Ideally Port 162 should be used to send receive Trap, any other available
	// Port can be used
	public static final int port = 162;

	public static void main(String[] args) throws Exception {

		// doMockGnocchiOperations();

		// invokeGnoochiAPIForMetrics(generateToken());

		sendTrapToNagios("60", nagiosIPAddress, "HomeStead having high CPU");
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

	public static void doMockGnocchiOperations() throws Exception {

		System.out.println("\n");
		System.out.println("\n");

		System.out.println("\n27-Mar-2018 17:30:33.720 Clearwater VM CPU usage data collected using Gnocchi is:");
		System.out.println("Homestead-1: 30% ");
		System.out.println("Nameserver: 22% ");
		System.out.println("Ellis-0: 27% ");
		System.out.println("Bono-0: 37% ");
		System.out.println("Sprout-0: 28% ");
		System.out.println("Ralf-0: 32% ");
		System.out.println("Homer: 12% ");
		System.out.println("Clearwater VMs' CPU usage is under control \n\n");

		System.out.println("27-Mar-2018 17:32:34.250 Clearwater VM CPU usage data collected using Gnocchi is:");
		System.out.println("Homestead-1: 38% ");
		System.out.println("Nameserver: 27% ");
		System.out.println("Ellis-0: 9% ");
		System.out.println("Bono-0: 18% ");
		System.out.println("Sprout-0: 19% ");
		System.out.println("Ralf-0: 33% ");
		System.out.println("Homer: 21% ");
		System.out.println("Clearwater VMs' CPU usage is under control");

		Thread.sleep(5000);

		System.out.println("\n27-Mar-2018 17:34:22.150 Clearwater VM CPU usage data collected using Gnocchi is:");
		System.out.println("Homestead-1: 18% ");
		System.out.println("Nameserver: 35% ");
		System.out.println("Ellis-0: 9% ");
		System.out.println("Bono-0: 14% ");
		System.out.println("Sprout-0: 34% ");
		System.out.println("Ralf-0: 25% ");
		System.out.println("Homer: 17% ");
		System.out.println("Clearwater VMs' CPU usage is under control \n\n");

		System.out.println("27-Mar-2018 17:36:15.256 Clearwater VM CPU usage data collected using Gnocchi is:");
		System.out.println("Homestead-1: 26% ");
		System.out.println("Nameserver: 18% ");
		System.out.println("Ellis-0: 19% ");
		System.out.println("Bono-0: 37% ");
		System.out.println("Sprout-0: 26% ");
		System.out.println("Ralf-0: 23% ");
		System.out.println("Homer: 21% ");
		System.out.println("Clearwater VMs' CPU usage is under control");

	}

	public static void invokeGnocchiAPIForMetrics(String token) {

		try {

			// this api is to invoke Openstack Ceilometer API with Keystone token provided
			// URL url = new URL("http://10.53.214.138:8777/v2/resources");
			// URL url = new
			// URL("http://10.53.214.138:8041/v1/aggregates?start=2018-03-23T17:30&stop=2018-03-25T01:00&groupby=original_resource_id&groupby=display_name&granularity=3600.0");

			URL url = new URL("http://10.53.214.138:8041/v1/metric");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("X-AUTH-TOKEN", token);

			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
			}

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

			String output;
			System.out.println("\n Result of invoking Openstack Ceilometer API .... \n");
			while ((output = br.readLine()) != null) {

				System.out.println(output);
			}

			conn.disconnect();

		} catch (MalformedURLException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();

		}

	}

	// this method intended to return keystone token
	public static String generateToken() {

		JSONObject jObj = null;
		JSONObject access = null;
		JSONObject token = null;

		try {
			// hitting KeyStone API and get the token by providing authentication
			URL url = new URL("http://10.53.214.138:35357/v2.0/tokens");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-Type", "application/json");

			String input = "{\"auth\":{\"passwordCredentials\":{\"username\": \"admin\", \"password\": \"FndtDuHTWzZYxBQ7BgW26PD7z\"}}}";
			OutputStream os = conn.getOutputStream();
			os.write(input.getBytes());
			os.flush();

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

			String output;
			StringBuffer response = new StringBuffer();
			System.out.println("\n Token details retrieved from Keystone API .... \n");

			while ((output = br.readLine()) != null) {

				System.out.println(output);
				response.append(output);
			}

			jObj = new JSONObject(response.toString());
			access = jObj.getJSONObject("access");
			token = access.getJSONObject("token");
			System.out.println("id-> " + token.getString("id"));

			conn.disconnect();

		} catch (MalformedURLException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();

		}

		return token.getString("id");

	}

}
