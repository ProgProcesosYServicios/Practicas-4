package es.pps.sockets.salachat;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;



//--------------------------------------------------------
//--------------------------------------------------------

/**
* Ventana de chat. Recibimos en el constructor un socket ya conectado con el
* servidor, y el nombre del usuario actual. Configuramos la ventana (heredamos
* de JFrame) para incluir un área de texto donde mostraremos los mensajes del
* chat, y un cuadro de texto donde el usuario escribe lo que quiere enviar.
* 
* Cada vez que detectamos que el usuario ha pulsado intro en el cuadro de
* texto, escribimos el texto directamente en el área de texto, y luego lo
* enviamos por el socket añadiéndole por delante la cadena "[<nombreUsuario>]
* ". Además, en el momento de creación del objeto, enviamos por el socket
* "[<nombreUsuario> CONECTADO]" y un mensaje similar cuando el usuario cierra
* la ventana.
* 
* Se utiliza un objeto de la clase RecibeYEscribe que se lanza en una hebra
* secundaria para leer líneas contínuamente del socket. Cada vez que la hebra
* detecta una, llama al método onTextoRecibido() de esta clase para que se le
* muestre al usuario.
* 
* @author Pedro Pablo Gómez Martín
*/
public class Chat extends JFrame {

	/**
	 * Constructor.
	 * 
	 * @param socket        Socket conectado con el servidor.
	 * @param nombreUsuario Nombre del usuario dentro del chat.
	 */
	
	
	public Chat(Socket socket, String nombreUsuario,  LinkedList<PrintWriter> writers) {

		super(nombreUsuario + " - Ventana de chat");
		_nombreUsuario = nombreUsuario;

		inicializarVentana();

		// Obtenemos el canal de salida donde mandaremos las
		// cadenas al servidor (desde enviarServidor(String) ).
		try {
			_canalSalida = new PrintWriter(socket.getOutputStream());
		} catch (IOException e) {
			anyadeTexto("  [Error de conexión]");
			return;
		}

		// Preparamos la hebra que se mantendrá a la escucha
		// del canal de entrada del socket y mandará todas las
		// líneas a nuestro método onTextoRecibido().
		RecibeYEscribe rye;
		try {
			rye = new RecibeYEscribe(socket.getInputStream(), _canalSalida, writers );
		} catch (IOException e) {
			anyadeTexto("  [Error de conexión]");
			return;
		}
		new Thread(rye).start();

		enviarServidor(" [CONECTADO]");

	} // Constructor

	// --------------------------------------------------------

	/**
	 * Método llamado externamente cuando se recibe texto por el socket que se debe
	 * mostrar al usuario. Se añade el texto al área de texto.
	 * 
	 * Ten en cuenta que este método no se llama cuando es el usuario el que ha
	 * escrito algo.
	 * 
	 * @param texto Texto recibido.
	 */
	public void onTextoRecibido(final String texto) {

		// No podemos llamar a Swing alegremente desde
		// otra hebra. Le pedimos que nos invoque más
		// adelante a través de un Runnable.
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				anyadeTexto(texto);
			}
		});

	} // onTextoRecibido

	// --------------------------------------------------------

	/**
	 * Añade al área de texto del chat el texto que se recibe como parámetro. Hace
	 * desplazarse también a la barra de desplazamiento para que esté siempre abajo.
	 * 
	 * @param texto Texto a añadir. Debe acabar en \n
	 */
	protected void anyadeTexto(String texto) {

		_taChat.append(texto);
		_taChat.setCaretPosition(_taChat.getDocument().getLength());

	} // anyadeTexto

	// --------------------------------------------------------

	/**
	 * Evento llamado cuando el usuario pulsa intro en el cuadro de texto para
	 * enviar lo que ha escrito.
	 */
	protected void onTextoEscrito() {

		String textoUsuario = _tfEntradaUsuario.getText();
		if (textoUsuario != null) {
			// El usuario ha escrito texto. Lo añadimos al área
			// de texto, y borramos el cuadro de texto del
			// usuario.
			anyadeTexto("[Yo] " + textoUsuario + "\n");
			_tfEntradaUsuario.setText("");
			// Y ahora lo mandamos al servidor.
			enviarServidor(textoUsuario);
		}

	} // onTextoEscrito

	// --------------------------------------------------------

	/**
	 * Envía por el socket al servidor el texto recibido como parámetro, añadiéndole
	 * delante el nombre del usuario.
	 * 
	 * @param texto Texto a añadir. Debe contener el "\n".
	 */
	protected void enviarServidor(String texto) {

		_canalSalida.println("[" + _nombreUsuario + "] " + texto);
		_canalSalida.flush(); // Forzamos el envío.

	} // enviarServidor

	// --------------------------------------------------------

	/**
	 * Método llamado desde el constructor para inicializar el interfaz gráfico del
	 * JFrame.
	 */
	protected void inicializarVentana() {

		// Establecemos el tamaño predefinido.
		setSize(300, 420);

		// Creamos el área de texto de la ventana del
		// chat.
		_taChat = new JTextArea();
		// El área de texto no será editable...
		_taChat.setEditable(false);
		// ... queremos que el ancho de sus líneas se ajusten
		// al ancho de la ventana ...
		_taChat.setLineWrap(true);
		// ... y que en la medida de lo posible no se
		// corten palabras, sino que se utilice el espacio
		// como separador para el cambio de línea.
		_taChat.setWrapStyleWord(true);

		// El área de texto lo vamos a "decorar" en un
		// panel que muestre, cuando sea necesario, las
		// barras de desplazamiento.
		JScrollPane scrollPane = new JScrollPane(_taChat);

		// Creamos un cuadro de texto donde el usuario
		// escribirá lo que quiere mandar.
		_tfEntradaUsuario = new JTextField();

		// Ponemos como disposición de los controles
		// del JFrame un BorderLayout...
		setLayout(new BorderLayout());
		// ... poniendo en el centro el área de texto
		// del chat ...
		add(scrollPane, BorderLayout.CENTER);
		// ... y en e sur el cuadro de texto donde el
		// usuario escribe.
		add(_tfEntradaUsuario, BorderLayout.SOUTH);

		// Si el usuario pulsa intro en el cuadro
		// de texto, llamamos al método onTextoEscrito().
		_tfEntradaUsuario.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onTextoEscrito();
			}
		});
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {

				enviarServidor(" [DESCONECTADO]");

				_canalSalida.close();

			}
		});
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// Mostramos la ventana y damos el foco al cuadro
		// de texto.
		setVisible(true);
		_tfEntradaUsuario.requestFocusInWindow();

	} // inicializarVentana

	// --------------------------------------------------------

	/**
	 * Programa principal. Interpreta los parámetros crea el socket con el servidor
	 * y lanza la ventana de chat.
	 * 
	 * @param args Parámetros en la línea de órdenes. El primero debe ser el nombre
	 *             del servidor, el segundo el puerto, y el tercero el nombre con el
	 *             que se conocerá al usuario en el chat.
	 */
/*
	public static void main(String[] args) {

		Socket socket;
		String hostname;
		String nombreUsuario;
		int port;

		// Analizamos los parámetros.
		if (args.length < 3) {
			// System.err.println("Faltan parámetros: <host> <puerto> <nombre>");
			// return;
			port = 4567;
			hostname = "localhost";
			nombreUsuario = "Bob";
		} else {
			hostname = args[0];
			try {
				port = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.err.println("Puerto inválido");
				return;
			}
			nombreUsuario = args[2];
		}
		// Intentamos conectarnos al servidor solicitado.
		try {
			socket = new Socket(hostname, port);
		} catch (UnknownHostException uhe) {
			System.err.println("No se pudo resolver " + hostname);
			return;
		} catch (IOException ioe) {
			System.err.println("Error de E/S al crear el socket: " + ioe.getLocalizedMessage());
			return;
		}

		new Chat(socket, nombreUsuario);

	} // main
*/
	
	public static void run(int port, String hostname,String nombreUsuario, LinkedList<PrintWriter> writers) {

		Socket socket;	
			
		// Intentamos conectarnos al servidor solicitado.
		try {
			socket = new Socket(hostname, port);
		} catch (UnknownHostException uhe) {
			System.err.println("No se pudo resolver " + hostname);
			return;
		} catch (IOException ioe) {
			System.err.println("Error de E/S al crear el socket: " + ioe.getLocalizedMessage());
			return;
		}

		new Chat(socket, nombreUsuario, writers);

	} // main
	// --------------------------------------------------------
	// Atributos protegidos/privados
	// --------------------------------------------------------

	/**
	 * JFrame implementa el interfaz serializable; necesitamos el identificador
	 * único de versión para la serialización.
	 */
	private static final long serialVersionUID = -2565821798757834656L;

	/**
	 * Area de texto con el texto del chat. Cada vez que se envía algo o se recibe
	 * algo, se añade aquí.
	 */
	JTextArea _taChat;

	/**
	 * Cuadro de texto donde el usuario escribe lo que quiere enviar.
	 */
	JTextField _tfEntradaUsuario;

	/**
	 * Nombre del usuario que está usando la aplicación. Se recibe como parámetro
	 * del programa.
	 */
	String _nombreUsuario;

	/**
	 * Canal de salida del socket por donde mandamos lo que el usuario escribe.
	 */
	PrintWriter _canalSalida;

} // Chat
