package es.pps.sockets.clientesimplex;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * Programa sencillo que se conecta por TCP a un host y puerto
 * especificados como parámetros al programa, y que envía todo lo
 * que se escribe por teclado.
 * 
 * No atiende a los datos que lleguen desde el otro extremo.
 * 
 * @author Pedro Pablo Gómez Martín
 */
public class ClienteSimplex {

	/**
	 * Método auxiliar que recibe una dirección de internet y un
	 * puerto y lo escribe por la salida estándar.
	 * 
	 * @param address Dirección de internet
	 * @param port Puerto
	 */
	protected static void escribeExtremo(InetAddress address, int port) {

		System.out.print(address.getHostAddress());
		System.out.print(":" + port);
		if (address.getCanonicalHostName() != null)
		System.out.print(" (" + address.getCanonicalHostName() + ")");

	} // escribeExtremo

	//------------------------------------------------

	/**
	 * Programa principal.
	 * 
	 * @param args Argumentos en la línea de órdenes. El primero
	 * debe ser el nombre del host al que conectarnos (o la IP) y
	 * el segundo el número de puerto.
	 */
	public static void main(String[] args) {

		Socket socket;
		String hostname;
		int port;

		// Analizamos los parámetros.
		if (args.length < 2) {
			System.err.println("Faltan parámetros: <host> <puerto>");
			return;
		}
		hostname = args[0];
		try {
			port = Integer.parseInt(args[1]);
		}
		catch(NumberFormatException e) {
			System.err.println("Puerto inválido");
			return;
		}

		// Intentamos conectarnos al servidor solicitado.
		try {
			socket = new Socket(hostname, port);
		}
		catch(UnknownHostException uhe) {
			System.err.println("No se pudo resolver " + hostname);
			return;
		}
		catch(IOException ioe) {
			System.err.println("Error de E/S al crear el socket: " + ioe.getLocalizedMessage());
			return;
		}

		// Mostramos información de la conexión.
		System.out.print("Conectado desde ");
		escribeExtremo(socket.getLocalAddress(), socket.getLocalPort());
		System.out.print(" a ");
		escribeExtremo(socket.getInetAddress(), socket.getPort());
		System.out.println();

		// Obtenemos el canal de escritura del socket para mandar texto
		// al servidor.
		PrintWriter out;
		try {
			out = new PrintWriter(socket.getOutputStream());
		} catch (IOException e) {
			System.err.println("No pude conseguir el canal de escritura del socket.");
			try {
				socket.close();
			} catch (IOException ioe) {
			}
			return;
		}

		// Leemos del teclado y mandamos por el socket todo lo
		// que recibimos.
		Scanner scanner = new Scanner(System.in);
	
		// Damos vueltas mientras el canal de salida esté funcionando y
		// haya más líneas que leer
		while (!out.checkError() && scanner.hasNextLine()) {
			String s = scanner.nextLine();
			out.println(s);
			out.flush(); // Forzamos el envío.
		} // while

		try {
			out.close();
			socket.close();
		} catch (IOException e) {
		}
		scanner.close();

	} // main

} // class ClienteSimplex