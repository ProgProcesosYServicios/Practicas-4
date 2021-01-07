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
* de JFrame) para incluir un �rea de texto donde mostraremos los mensajes del
* chat, y un cuadro de texto donde el usuario escribe lo que quiere enviar.
* 
* Cada vez que detectamos que el usuario ha pulsado intro en el cuadro de
* texto, escribimos el texto directamente en el �rea de texto, y luego lo
* enviamos por el socket a�adi�ndole por delante la cadena "[<nombreUsuario>]
* ". Adem�s, en el momento de creaci�n del objeto, enviamos por el socket
* "[<nombreUsuario> CONECTADO]" y un mensaje similar cuando el usuario cierra
* la ventana.
* 
* Se utiliza un objeto de la clase RecibeYEscribe que se lanza en una hebra
* secundaria para leer l�neas cont�nuamente del socket. Cada vez que la hebra
* detecta una, llama al m�todo onTextoRecibido() de esta clase para que se le
* muestre al usuario.
* 
* @author Pedro Pablo G�mez Mart�n
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
			anyadeTexto("  [Error de conexi�n]");
			return;
		}

		// Preparamos la hebra que se mantendr� a la escucha
		// del canal de entrada del socket y mandar� todas las
		// l�neas a nuestro m�todo onTextoRecibido().
		RecibeYEscribe rye;
		try {
			rye = new RecibeYEscribe(socket.getInputStream(), _canalSalida, writers );
		} catch (IOException e) {
			anyadeTexto("  [Error de conexi�n]");
			return;
		}
		new Thread(rye).start();

		enviarServidor(" [CONECTADO]");

	} // Constructor

	// --------------------------------------------------------

	/**
	 * M�todo llamado externamente cuando se recibe texto por el socket que se debe
	 * mostrar al usuario. Se a�ade el texto al �rea de texto.
	 * 
	 * Ten en cuenta que este m�todo no se llama cuando es el usuario el que ha
	 * escrito algo.
	 * 
	 * @param texto Texto recibido.
	 */
	public void onTextoRecibido(final String texto) {

		// No podemos llamar a Swing alegremente desde
		// otra hebra. Le pedimos que nos invoque m�s
		// adelante a trav�s de un Runnable.
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				anyadeTexto(texto);
			}
		});

	} // onTextoRecibido

	// --------------------------------------------------------

	/**
	 * A�ade al �rea de texto del chat el texto que se recibe como par�metro. Hace
	 * desplazarse tambi�n a la barra de desplazamiento para que est� siempre abajo.
	 * 
	 * @param texto Texto a a�adir. Debe acabar en \n
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
			// El usuario ha escrito texto. Lo a�adimos al �rea
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
	 * Env�a por el socket al servidor el texto recibido como par�metro, a�adi�ndole
	 * delante el nombre del usuario.
	 * 
	 * @param texto Texto a a�adir. Debe contener el "\n".
	 */
	protected void enviarServidor(String texto) {

		_canalSalida.println("[" + _nombreUsuario + "] " + texto);
		_canalSalida.flush(); // Forzamos el env�o.

	} // enviarServidor

	// --------------------------------------------------------

	/**
	 * M�todo llamado desde el constructor para inicializar el interfaz gr�fico del
	 * JFrame.
	 */
	protected void inicializarVentana() {

		// Establecemos el tama�o predefinido.
		setSize(300, 420);

		// Creamos el �rea de texto de la ventana del
		// chat.
		_taChat = new JTextArea();
		// El �rea de texto no ser� editable...
		_taChat.setEditable(false);
		// ... queremos que el ancho de sus l�neas se ajusten
		// al ancho de la ventana ...
		_taChat.setLineWrap(true);
		// ... y que en la medida de lo posible no se
		// corten palabras, sino que se utilice el espacio
		// como separador para el cambio de l�nea.
		_taChat.setWrapStyleWord(true);

		// El �rea de texto lo vamos a "decorar" en un
		// panel que muestre, cuando sea necesario, las
		// barras de desplazamiento.
		JScrollPane scrollPane = new JScrollPane(_taChat);

		// Creamos un cuadro de texto donde el usuario
		// escribir� lo que quiere mandar.
		_tfEntradaUsuario = new JTextField();

		// Ponemos como disposici�n de los controles
		// del JFrame un BorderLayout...
		setLayout(new BorderLayout());
		// ... poniendo en el centro el �rea de texto
		// del chat ...
		add(scrollPane, BorderLayout.CENTER);
		// ... y en e sur el cuadro de texto donde el
		// usuario escribe.
		add(_tfEntradaUsuario, BorderLayout.SOUTH);

		// Si el usuario pulsa intro en el cuadro
		// de texto, llamamos al m�todo onTextoEscrito().
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
	 * Programa principal. Interpreta los par�metros crea el socket con el servidor
	 * y lanza la ventana de chat.
	 * 
	 * @param args Par�metros en la l�nea de �rdenes. El primero debe ser el nombre
	 *             del servidor, el segundo el puerto, y el tercero el nombre con el
	 *             que se conocer� al usuario en el chat.
	 */
/*
	public static void main(String[] args) {

		Socket socket;
		String hostname;
		String nombreUsuario;
		int port;

		// Analizamos los par�metros.
		if (args.length < 3) {
			// System.err.println("Faltan par�metros: <host> <puerto> <nombre>");
			// return;
			port = 4567;
			hostname = "localhost";
			nombreUsuario = "Bob";
		} else {
			hostname = args[0];
			try {
				port = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.err.println("Puerto inv�lido");
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
	 * �nico de versi�n para la serializaci�n.
	 */
	private static final long serialVersionUID = -2565821798757834656L;

	/**
	 * Area de texto con el texto del chat. Cada vez que se env�a algo o se recibe
	 * algo, se a�ade aqu�.
	 */
	JTextArea _taChat;

	/**
	 * Cuadro de texto donde el usuario escribe lo que quiere enviar.
	 */
	JTextField _tfEntradaUsuario;

	/**
	 * Nombre del usuario que est� usando la aplicaci�n. Se recibe como par�metro
	 * del programa.
	 */
	String _nombreUsuario;

	/**
	 * Canal de salida del socket por donde mandamos lo que el usuario escribe.
	 */
	PrintWriter _canalSalida;

} // Chat
