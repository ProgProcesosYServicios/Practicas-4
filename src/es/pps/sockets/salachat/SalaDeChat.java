package es.pps.sockets.salachat;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

/**
 * Clase principal de la aplicaci�n. Es el lado del servidor de una sala de
 * chat. Permite que se le conecten tantos clientes como se quiera. Para cada
 * uno, se queda escuchando en su stream de entrada y reenv�a todo lo que le
 * llega a todos los dem�s, sin modificarlo.
 * 
 * En la salida est�ndar muestra, por depuraci�n, todos los mensajes recibidos,
 * as� como informaci�n sobre cada conexi�n que nos llega.
 * 
 * @author Pedro Pablo G�mez Mart�n
 */
public class SalaDeChat {

	/**
	 * M�todo auxiliar que recibe una direcci�n de internet y un puerto y lo escribe
	 * por la salida est�ndar.
	 * 
	 * @param address Direcci�n de internet
	 * @param port    Puerto
	 */
	protected static void escribeExtremo(InetAddress address, int port) {

		System.out.print(address.getHostAddress());
		System.out.print(":" + port);
		if (address.getCanonicalHostName() != null)
			System.out.print(" (" + address.getCanonicalHostName() + ")");

	} // escribeExtremo

	// ------------------------------------------------

	/**
	 * Programa principal.
	 * 
	 * @param args Argumentos recibidos de la l�nea de �rdenes. Debe haber uno, con
	 *             el n�mero de puerto donde escuchar.
	 */
	public static void main(String[] args) {

		Socket socket;
		int port;

		// Analizamos los par�metros.
		if (args.length < 1) {
			// System.err.println("Falta el n�mero de puerto.");
			port = 4567;
			// return;
		} else
			try {
				port = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				System.err.println("Puerto inv�lido");
				return;
			}

		// Creamos el ServerSocket donde nos quedaremos
		// escuchando.
		try (ServerSocket serverSocket = new ServerSocket(port)) {

			// Creamos la lista de "writers" (el lado de salida de
			// todos los sockets que se nos han conectado), inicialmente
			// vac�a.
			List<PrintWriter> writers;
			writers = new LinkedList<PrintWriter>();

			// Damos vueltas cont�nuamente.
			while (true) {

				// Esperamos el siguiente cliente.
				try {
					socket = serverSocket.accept();
				} catch (IOException ioe) {
					System.err.println("Error esperando clientes: " + ioe.getLocalizedMessage());

					return;
				}

				// Acaba de llegarnos un nuevo cliente.
				// Mostramos informaci�n de la conexi�n.
				System.out.print("[ Conexi�n desde ");
				escribeExtremo(socket.getLocalAddress(), socket.getLocalPort());
				System.out.print(" a ");
				escribeExtremo(socket.getInetAddress(), socket.getPort());
				System.out.println(" ]");

				// Obtenemos el canal de escritura del socket para mandar texto
				// a este cliente cuando cualquier otro escriba.
				PrintWriter out;
				try {
					out = new PrintWriter(socket.getOutputStream());
				} catch (IOException e) {
					System.err.println("No pude conseguir el canal de escritura del socket.");
					continue;
				}

				writers.add(out);

				// Lanzamos una hebra para escribir todo lo que nos llegue.
				RecibeYEscribe rye;
				try {
					rye = new RecibeYEscribe(socket.getInputStream(), out, writers);
				} catch (IOException e) {
					System.err.println("No pude conseguir el canal de lectura del socket.");
					return;
				}
				new Thread(rye).start();

			} // while(true)
		} catch (IOException e) {
			System.out.println("No pude escuchar en el puerto " + port);
			return;
		}
	} // main

} // SalaDeChat
