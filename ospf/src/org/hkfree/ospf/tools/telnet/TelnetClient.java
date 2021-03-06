package org.hkfree.ospf.tools.telnet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Třída představující telnet klienta pro připojení na router a práci s ním.
 * @author Jan Schovánek
 */
public class TelnetClient {

    /** Výstupní stream odesílání dat */
    private OutputStream _os = null;
    /** Vstupní stream pro přijetí dat */
    private InputStream _is = null;
    private Socket _socket;
    private String host = null;
    private int port;
    private int timeout;
    private String password = null;
    private StringBuilder _sb = null;


    /**
     * Konstruktor
     * @param host
     * @param port
     * @param password
     */
    public TelnetClient(String host, int port, String password, int timeout) {
	this.host = host;
	this.port = port;
	this.password = password;
	this.timeout = timeout;
    }


    /**
     * @throws TelnetException
     * @throws IOException
     *             Navázání spojení s routerem a přihlášení
     * @return příznak zda se spojení zdařilo
     */
    public StringBuilder initConnection() throws IOException, TelnetException   {
	if (timeout == 0) {
	    throw new TelnetException("Timeout must be greater than 0 ms.");
	}
	setSb(new StringBuilder());
	SocketAddress sockAddr = new InetSocketAddress(host, port);
	_socket = new Socket();
	_socket.connect(sockAddr, timeout);
	_os = _socket.getOutputStream();
	_is = _socket.getInputStream();
	while (!_sb.toString().endsWith("Password: ")) {
	    sendRecive("");// odeslu enter, kvuli srovnani prijatych/odeslanych dat
	}
	if (password != null) { // nemelo by byt nikdy nullove
	    sendRecive(this.password);
	}
	sendRecive("terminal length 0");
	// podruhe jiz prijata data nesmi koncit zadosti o heslo
	if (getSb().toString().endsWith("Password: ")) {
	    throw new TelnetException("Telnet connection error.");
	}
	return getSb();
    }


    /**
     * Odpojení telnetu
     * @throws IOException
     * @throws InterruptedException
     */
    public void close() {
	setSb(new StringBuilder());
	sendRecive("exit");
    }


    public StringBuilder getDataIPv4() {
	setSb(new StringBuilder());
	sendRecive("show ip ospf database network");
	sendRecive("show ip ospf database router");
	sendRecive("show ip ospf database external");
	return getSb();
    }


    public StringBuilder getDataIPv6() {
	setSb(new StringBuilder());
	sendRecive("show ipv6 ospf6 database network detail");
	sendRecive("show ipv6 ospf6 database router detail");
	sendRecive("show ipv6 ospf6 database as-external detail");
	return getSb();
    }


    /**
     * Odešle zprávu přes telnet na router a zastaví běh aktuálního threadu.
     * Až přijdou data, opět thread spustí a pokračuje dále ve vykonávání.
     * @param msg příkaz k odeslání
     * @throws IOException
     * @throws InterruptedException
     */
    private void sendRecive(String msg) {
	try {
	    // odeslu prikaz
	    _os.write(msg.getBytes());
	    _os.write("\n".getBytes());
	    _os.flush();
	    receiveData();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }


    /**
     * Příjem dat TEST
     */
    private void receiveData() {
	try {
	    Thread.sleep(50);
	} catch (InterruptedException e1) {
	    e1.printStackTrace();
	}
	byte[] buff = new byte[1024];
	int receiveLength = 0; // počet znaků přijatého řetězce
	while (true) {
	    try {
		receiveLength = _is.read(buff);
		if (receiveLength != -1) {
		    if (getSb() != null) {
			getSb().append(new String(buff, 0, receiveLength));
		    }
		}
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	    if (getSb() == null || getSb().toString().endsWith("> ") || receiveLength == -1
		    || getSb().toString().endsWith("Password: ")) {
		break;
	    }
	}
    }


    /**
     * Nastaví string builder pro uložení přijatých dat
     * @param sb
     */
    private void setSb(StringBuilder sb) {
	this._sb = sb;
    }


    /**
     * Vrací string bulider pro uložení přijatých dat
     * @return
     */
    private StringBuilder getSb() {
	return _sb;
    }
}
