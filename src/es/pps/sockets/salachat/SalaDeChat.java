package es.pps.sockets.salachat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

/**
 * Clase que implementa el interfaz Runnable para ser lanzada en otra hebra.
 * 
 * Recibe un InputStream como parámetro en el constructor y se queda
 * contínuamente leyendo líneas de él (a través de un BufferedReader). Cada vez
 * que recibe una línea la manda a todos los PrintWriter contenidos en una lista
 * recibida en el constructor, salvo a uno especial que es el asociado al mismo
 * socket que el InputStream de donde estamos leyendo.
 * 
 * @author Pedro Pablo Gómez Martín
 */
class RecibeYEscribe implements Runnable {

	/**
	 * Constructor
	 * 
	 * @param is      Stream de entrada del que leer líneas.
	 * @param pw      PrintWriter asociado al mismo socket que el stream del primer
	 *                parámetro.
	 * @param writers Lista con los canales de salida de todos los clientes.
	 */
	RecibeYEscribe(InputStream is, PrintWriter pw, List<PrintWriter> writers) {

		_reader = new BufferedReader(new InputStreamReader(is));
		_pw = pw;
		_writers = writers;

	} // Constructor

	/**
	 * Método para ser lanzado en otra hebra. Lee lineas del canal recibido en el
	 * constructor y las escribe por todos los PrintWriter de la lista recibida en
	 * el constructor.
	 */
	@Override
	public void run() {

		String leido;

		while (true) {
			try {
				leido = _reader.readLine();
			} catch (IOException e) {
				break;
			}
			if (leido == null) {
				// EOF.
				break;
			}
			for (PrintWriter cliente : _writers) {
				if (cliente != _pw) {
					// El PrintWriter siguiente no es el cliente al que
					// estamos escuchando aquí (para no mandarnos a
					// nosotros mismos lo que acabamos de enviar)
					cliente.println(leido);
					if (cliente.checkError())
						System.err.println("\t[Error en el último envío]");
				}
			}
			System.out.println(leido);
		} // while

		// Cerramos el canal de entrada. El socket se dará
		// cuenta, y verá que el canal de salida está
		// también cerrado (por el otro extremo) y
		// dará error en el próximo intento de escritura.
		try {
			_reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("  [Fin de la hebra de entrada]");

	} // run

	BufferedReader _reader;
	PrintWriter _pw;
	List<PrintWriter> _writers;

} // class RecibeYReenvia

//--------------------------------------------------------
//--------------------------------------------------------

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