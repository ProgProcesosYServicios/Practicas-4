package es.pps.sockets.salachat;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

/**
 * Clase principal de la aplicación. Es el lado del servidor de una sala de
 * chat. Permite que se le conecten tantos clientes como se quiera. Para cada
 * uno, se queda escuchando en su stream de entrada y reenvía todo lo que le
 * llega a todos los demás, sin modificarlo.
 * 
 * En la salida estándar muestra, por depuración, todos los mensajes recibidos,
 * así como información sobre cada conexión que nos llega.
 * 
 * @author Pedro Pablo Gómez Martín
 */
public class SalaDeChat {

	/**
	 * Método auxiliar que recibe una dirección de internet y un puerto y lo escribe
	 * por la salida estándar.
	 * 
	 * @param address Dirección de internet
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
	 * @param args Argumentos recibidos de la línea de órdenes. Debe haber uno, con
	 *             el número de puerto donde escuchar.
	 */
	public static void main(String[] args) {

		Socket socket;
		int port;

		// Analizamos los parámetros.
		if (args.length < 1) {
			// System.err.println("Falta el número de puerto.");
			port = 4567;
			// return;
		} else
			try {
				port = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				System.err.println("Puerto inválido");
				return;
			}

		// Creamos el ServerSocket donde nos quedaremos
		// escuchando.
		try (ServerSocket serverSocket = new ServerSocket(port)) {

			// Creamos la lista de "writers" (el lado de salida de
			// todos los sockets que se nos han conectado), inicialmente
			// vacía.
			List<PrintWriter> writers;
			writers = new LinkedList<PrintWriter>();

			// Damos vueltas contínuamente.
			while (true) {

				// Esperamos el siguiente cliente.
				try {
					socket = serverSocket.accept();
				} catch (IOException ioe) {
					System.err.println("Error esperando clientes: " + ioe.getLocalizedMessage());

					return;
				}

				// Acaba de llegarnos un nuevo cliente.
				// Mostramos información de la conexión.
				System.out.print("[ Conexión desde ");
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
