#include <stdio.h>
#include <string.h>
#include <errno.h>
#ifdef __gnu_linux__
	#include <sys/types.h>
	#include <sys/socket.h>
	#include <netinet/in.h>
	#include <unistd.h>
	#include <stdlib.h>
	#include <netdb.h>
	#include <arpa/inet.h>
	#include <pthread.h>
	#include <netinet/tcp.h>
#elif _WIN32
	#include <winsock2.h>
	#include <windows.h>
	#include <ws2tcpip.h>
	#pragma comment(lib, "Ws2_32.lib ")
	#pragma comment(lib, "wsock32.lib")
#else
	#error OS not supported, have fun with adding ifdefs
#endif

int learner_listener_sd, learner_conn_sd, main_sd, secondary_sd;
#ifdef _WIN32
HANDLE socket_thread;
#elif __gnu_linux__
pthread_t socket_thread;
#endif
int main_socket_blocked;

int server_port, learner_port;
char server_addr[100];

#define send_buf "x"

#define default_learner_port 5000
#define default_server_port 34567
#define default_server_addr "1.2.3.4"

#define input_buffer_size 1025
#define output_buffer_size 1025
char output_buffer[output_buffer_size];

#define syn_ok (0)
#define client_type (0)
#define server_type (1)
int type;

struct sockaddr_in serv_addr_struct;
struct sockaddr_in local_addr;

void error(char* msg) {
	printf("error: %s\naborting\n", msg);
	exit(-1);
}

void strcpy_end(char* dest, char* src, int maxsize) {
	int len = strlen(src) + 1;
	if (maxsize < len) {
		len = maxsize;
	}
	if (len == 0) {
		error("cannot copy 0 characters safely!"); // not very subtle, but I want to know these things
	}
#ifdef _WIN32
	strcpy_s(dest, len, src);
#else
	strncpy(dest, src, len - 1);
#endif
	dest[len-1] = '\0';
}

void answer(char* output) {
	send(learner_conn_sd, output, strlen(output), 0);
}

//convert newline-terminated input to null-terminated input
void str_network_to_c(char* string) {
	unsigned int i;
	for (i = 0; i < strlen(string); i++) {
		if (string[i] == '\n' || string[i] == '\r') {
			string[i] = '\0';
			return;
		}
	}
}

void wait_ok() {
	if (syn_ok) {
		char read_buffer[input_buffer_size];
	#ifdef _WIN32
		int result = recv(learner_conn_sd, read_buffer, sizeof(read_buffer), 0);
	#elif __gnu_linux__
		int result = read(learner_conn_sd, read_buffer, sizeof(read_buffer));
	#endif
		if (result <= 0) { // either -1 for an error, or 0 if connection is closed properly
			error("expected 'ok' but could not read socket input\n");
		}
		str_network_to_c(read_buffer);
		

		if (strncmp(read_buffer, "ok", sizeof(read_buffer)) == 0) {
			printf("received permission\n");
		} else {
			char buf[1024];
			sprintf(buf, "expected ok, received %s\n",read_buffer);
			error(buf);
		}
	}
}

void send_ok() {
	if (syn_ok) {
		answer("ok\n");
	}
}

#ifdef _WIN32
DWORD WINAPI do_connect(void *arg)
#elif __gnu_linux__
void *do_connect(void *arg)
#endif
{
	send_ok();
	wait_ok();
	if (connect(main_sd, (struct sockaddr *) &serv_addr_struct, sizeof(serv_addr_struct)) < 0) {
        printf("connect failed\n");
	} else {
		printf("connect succeeded\n");
		int i = 1;
		setsockopt(main_sd, IPPROTO_TCP, TCP_NODELAY, (void *)&i, sizeof(i));
#ifdef __gnu_linux__
		setsockopt(secondary_sd, IPPROTO_TCP, TCP_QUICKACK, (void *)&i, sizeof(i));
#endif
	}
	main_socket_blocked = 0;
	return 0;
}

void start_connect_thread() {
	if (main_socket_blocked == 0 && secondary_sd == -1) {
		main_socket_blocked = 1;
#ifdef _WIN32
		socket_thread = CreateThread(NULL, 0, &do_connect, NULL, 0, NULL);
#elif __gnu_linux__
		pthread_create(&socket_thread, NULL, &do_connect, NULL);
#else
		printf("cannot accept process, unknown OS\n");
#endif
	} else {
		send_ok();
		wait_ok();
	}
}

void stop_thread() {
	if (main_socket_blocked) {
#ifdef _WIN32
		TerminateThread(socket_thread, 0);
#elif __gnu_linux__
		pthread_cancel(socket_thread);
#endif
	}
	main_socket_blocked = 0;
}

#ifdef _WIN32
DWORD WINAPI do_accept(void *arg) {
#elif __gnu_linux__
void *do_accept(void *arg) {
#endif
	//do {
	int new_connection = accept(main_sd, (struct sockaddr*)NULL, NULL);
	if (new_connection >= 0) {
		secondary_sd = new_connection;
		printf("accept succeeded\n");
		printf("created connection socket %d\n", new_connection);
		int i = 1;
		setsockopt(secondary_sd, IPPROTO_TCP, TCP_NODELAY, (void *)&i, sizeof(i));
#ifdef __gnu_linux__
		setsockopt(secondary_sd, IPPROTO_TCP, TCP_QUICKACK, (void *)&i, sizeof(i));
#endif
	} else { 
		printf("accept failed\n");
		//answer("NOK\n");
	}
	//} while (secondary_sd != -1);
	main_socket_blocked = 0;
	return 0;
}

#ifdef _WIN32
DWORD WINAPI do_recv(void *arg) {
#elif __gnu_linux__
void *do_recv(void *arg) {
#endif
	int sd = -1;
	if (type == server_type) {
		sd = secondary_sd;
	}
	else if (type == client_type) {
		sd = main_sd;
	}
	if (sd != -1) {
		char read_buffer[input_buffer_size];
		recv(sd, read_buffer, sizeof(read_buffer), 0);
		printf("receive succeeded\n");
	}
	else {
		printf("connection socket not defined\n");
	}
	main_socket_blocked = 0;
}

void start_recv_thread() {
	if (main_socket_blocked == 0) {
		main_socket_blocked = 1;
#ifdef _WIN32
		socket_thread = CreateThread(NULL, 0, &do_recv, NULL, 0, NULL);
#elif __gnu_linux__
		pthread_create(&socket_thread, NULL, &do_recv, NULL);
#else
		printf("cannot accept process, unknown OS\n");
#endif
	}
}

void start_accept_thread() {
	if (main_socket_blocked == 0 && secondary_sd == -1) {
		main_socket_blocked = 1;
#ifdef _WIN32
		socket_thread = CreateThread(NULL, 0, &do_accept, NULL, 0, NULL);
#elif __gnu_linux__
		pthread_create(&socket_thread, NULL, &do_accept, NULL);
#else
		printf("cannot accept process, unknown OS\n");
#endif
	}
	else {
		if (main_socket_blocked == 1) {
			printf("accept failed, server socket blocked");
		}
		else {
			if (secondary_sd != -1) {
				printf("accept failed, connection socket (%d) has already been created", secondary_sd);
			}
		}
	}
}

void init() {
	#ifdef _WIN32
		WSADATA wsaData;
		WSAStartup(0x0202, &wsaData);
	#endif
	learner_listener_sd = socket(AF_INET, SOCK_STREAM, 0);
	struct sockaddr_in learner_addr;
	memset(&learner_addr, 0, sizeof(struct sockaddr_in));
	learner_addr.sin_family = AF_INET; 
	learner_addr.sin_addr.s_addr = htonl(INADDR_ANY);
	learner_addr.sin_port = htons(learner_port);
    int yes = 1;
    setsockopt(learner_listener_sd, SOL_SOCKET, SO_REUSEADDR, &yes, sizeof(int));
	bind(learner_listener_sd, (struct sockaddr*)&learner_addr, sizeof(learner_addr));
	
	if (listen(learner_listener_sd, 1) != 0) {
		error("cannot open socket to listen to learner\n");
	}
	printf("listen for learner...\n");
	learner_conn_sd = accept(learner_listener_sd, (struct sockaddr*)NULL, NULL);
	while (learner_conn_sd == -1) {
		printf("could not establish connection with learner, retrying...\n");
		#ifdef _WIN32
		Sleep(1);
		#elif __gnu_linux__
		sleep(1);
		#endif
		learner_conn_sd = accept(learner_listener_sd, (struct sockaddr*)NULL, NULL);
	}
	printf("established connection with learner!\n");
}

void init_run() {
	main_socket_blocked = 0;
	type = -1;
	printf("*** NEW RUN ***\n");
	printf("creating socket...\n");
	main_sd = socket(AF_INET, SOCK_STREAM, 0);
	printf("finished creating socket!\n");
	if (main_sd < 0) {
		printf("creating socket failed\n");
		error("could not open new socket\n");
	}
	
	printf("initializing server address\n");
	struct hostent *server = gethostbyname(server_addr);
	memset((char *)&serv_addr_struct, 0, sizeof(serv_addr_struct));
    serv_addr_struct.sin_family = AF_INET;
    memcpy((char *)&serv_addr_struct.sin_addr.s_addr,
		(char *)server->h_addr,
		server->h_length);
    serv_addr_struct.sin_port = htons(server_port);

	printf("initializing client address\n");
	memset(&local_addr, 0, sizeof(struct sockaddr_in));
    local_addr.sin_family = AF_INET;
    local_addr.sin_port = htons(0);
    
	#ifdef _WIN32
	int int_addr = InetPton(AF_INET, "0.0.0.0", &local_addr.sin_addr.s_addr);
	#elif __gnu_linux__
	int int_addr = inet_pton(AF_INET, "0.0.0.0", &local_addr.sin_addr.s_addr);
	#endif

	if (int_addr <= 0) {
		if (int_addr == 0)
			error("Client address not a valid address");
		else
			error("Could not convert address");
	}
    
    int assigned_port;
    if (bind(main_sd, (struct sockaddr *)&local_addr, sizeof(struct sockaddr)) == -1) {
		error("could not bind local socket to random port number");
	} else {
		struct sockaddr_in infoaddr;
		int infolen = sizeof(infoaddr);
		getsockname(main_sd, (struct sockaddr*) &infoaddr, &infolen);
		//(struct sockaddr_in*) infoaddr_pointer = (struct sockaddr_in*) infoaddr;
		assigned_port = ntohs(infoaddr.sin_port);
		printf("bound to randomly assigned port %i\n", assigned_port);
	}
	char buf[100];
	sprintf(buf, "port %i\n", assigned_port);
	answer(buf);
	printf("\nfinished initialization!\n");
}

void close_run() {
#ifdef _WIN32
	closesocket(main_sd);
	closesocket(secondary_sd);
#elif __gnu_linux__	
	char msg[200];
	snprintf(msg, sizeof(msg), "socket descriptors:\n1: %i\n2: %i\n", 
		main_sd, secondary_sd);
		printf("%s", msg);
	if (main_sd != -1 && close(main_sd) != 0) {
		if (errno == EBADF) {
			printf("errno = EBADF\n");
		} else if (errno == EINTR) {
			printf("errno = EINTR\n");
		} else if (errno == EIO) {
			printf("errno = EIO\n");
		} else {
			printf("errno = %i\n", errno);
		}
		error("could not close main socket");
	}
	if (secondary_sd != -1 && close(secondary_sd) != 0) {
		if (errno == EBADF) {
			printf("errno = EBADF\n");
		} else if (errno == EINTR) {
			printf("errno = EINTR\n");
		} else if (errno == EIO) {
			printf("errno = EIO\n");
		} else {
			printf("errno = %i\n", errno);
		}
		error("could not close secondary socket");
	}
#endif
	main_sd = secondary_sd = -1;
}

void process_connect() {
	type = client_type;
	//stop_connect_thread();
	start_connect_thread();
}

void process_send() {
	int sd = -1;
	// find the connection socket, if any
	if (type == server_type) {
		sd = secondary_sd;
	} else if (type == client_type) {
		sd = main_sd;
	}
	if (sd != -1) {
		//send(SOCKET socket, const char * buffer, int buflen, int flags);
		if (send(sd, send_buf, strlen(send_buf), MSG_NOSIGNAL ) != -1) {
			printf("send succeeded\n");
		}
		else {
			printf("send failed\n");
		}
	}
}

void process_recv() {
	start_recv_thread();
}



void process_close() {
	send_ok();
	wait_ok();
	stop_thread();
	int ret;
#ifdef _WIN32
	ret = closesocket(main_sd);
#elif __gnu_linux__	
	ret = close(main_sd);
#endif
	if (ret == 0) {
		printf("close succeded\n");
	}
	else {
		printf("close failed\n");
	}
	main_sd = -1;
}

void process_close_secondary() {
	send_ok();
	wait_ok();
	stop_thread();
	int ret;
#ifdef _WIN32
	ret = closesocket(secondary_sd);
#elif __gnu_linux__	
	ret = close(secondary_sd);
#endif
	if (ret == 0) {
		printf("close succeded\n");
	}
	else {
		printf("close failed\n");
	}
	secondary_sd = -1;
}

void process_accept() {
	//stop_accept_thread();
	start_accept_thread();
}

void process_listen() {
	type = server_type;
	printf("LISTEN\n");
	if (listen(main_sd, 1) == 0) {
		//answer("OK\n");
		printf("listen succeeded\n");
	}
	else {
		//answer("NOK\n");
		printf("listen failed\n");
#ifdef __gnu_linux__
		int sendbuf;
		socklen_t sendbufsize = sizeof(sendbuf);
		int error = getsockopt(main_sd, SOL_SOCKET, SO_ERROR, &sendbuf, &sendbufsize);
		printf("error-code: %i or %i\n", error, sendbuf);
#elif _WIN32
		printf("no error-code for windows\n");
#endif
	}
}

int process_input() {
	char read_buffer[input_buffer_size];
#ifdef _WIN32
	int result = recv(learner_conn_sd, read_buffer, sizeof(read_buffer), 0);
#elif __gnu_linux__
	int result = read(learner_conn_sd, read_buffer, sizeof(read_buffer));
#endif
	if (result <= 0) { // either -1 for an error, or 0 if connection is closed properly
		return -1;
	}
	str_network_to_c(read_buffer);
	printf("received: %s\n", read_buffer);

	if (strncmp(read_buffer, "connect", sizeof(read_buffer)) == 0) {
		process_connect();
	}
	else if (strncmp(read_buffer, "close", sizeof(read_buffer)) == 0) {
		process_close();
	}
	else if (strncmp(read_buffer, "listen", sizeof(read_buffer)) == 0) {
		process_listen();
	}
	else if (strncmp(read_buffer, "accept", sizeof(read_buffer)) == 0) {
		process_accept();
	}
	else if (strncmp(read_buffer, "rcv", sizeof(read_buffer)) == 0) {
	 process_recv();
	}
	else if (strncmp(read_buffer, "closeconnection", sizeof(read_buffer)) == 0) {
		process_close_secondary();
	}
	else if (strncmp(read_buffer, "send", sizeof(read_buffer)) == 0) {
		process_send();
	}
	
	else if (strncmp(read_buffer, "reset", sizeof(read_buffer)) == 0) {
		close_run();
		send_ok();
		init_run();
	}
	else if (strncmp(read_buffer, "port", sizeof("port")-1) == 0) {
		char port_buf[100];
		strncpy(port_buf, &(read_buffer[sizeof("port")]), sizeof(port_buf));
		server_port = atoi(port_buf);
		printf("learner port set to %i\n", server_port);
	}
	else if (strncmp(read_buffer, "exit", sizeof(read_buffer)) == 0) {
		return -1;
	}
	else {
		printf("Unrecognized command %s. Exiting...", read_buffer);
		return -1;
	}
	return 0;
}

void run() {
	init_run();
	
	while(process_input() != -1); // stop if not succesfull, e.g. learner socket has closed.
	
	printf("learner disconnected, terminating\n");
	
	close_run();
	
	#ifdef _WIN32
		closesocket(learner_conn_sd);
	#elif __gnu_linux__
		close(learner_conn_sd);
	#endif
}

char* help = "[-c | --continuous] [-l learnerport] [--dport|-p portnumber] [--daddr|-a ip address]";
int main(int argc, char *argv[]) {
	learner_port = -1;
	learner_listener_sd = learner_conn_sd = main_sd = secondary_sd = -1;
	int arg_nr;
	int continuous = 0;
	strcpy_end(server_addr, default_server_addr, sizeof(server_addr));
	server_port = default_server_port;
	learner_port = default_learner_port;
	server_port = default_server_port;
	
	for (arg_nr = 1; arg_nr < argc; arg_nr++) {
		if (strcmp(argv[arg_nr], "--continuous") == 0 || strcmp(argv[arg_nr], "-c") == 0) {
			continuous = 1;
		} else if (strcmp(argv[arg_nr], "--learnerport") == 0 || strcmp(argv[arg_nr], "-l") == 0) {
			arg_nr++;
			if (arg_nr >= argc) {
				printf("argument %s needs extra parameter\n", argv[arg_nr-1]);
				return 2;
			}
			learner_port = atoi(argv[arg_nr]);
		} else if (strcmp(argv[arg_nr], "--dport") == 0 || strcmp(argv[arg_nr], "-p") == 0) {
			arg_nr++;
			if (arg_nr >= argc) {
				printf("argument %s needs extra parameter\n", argv[arg_nr-1]);
				return 2;
			}
			server_port = atoi(argv[arg_nr]);
		} else if (strcmp(argv[arg_nr], "--daddr") == 0 || strcmp(argv[arg_nr], "-a") == 0) {
			arg_nr++;
			if (arg_nr >= argc) {
				printf("argument %s needs extra parameter\n", argv[arg_nr-1]);
				return 2;
			}
			strcpy_end(server_addr, argv[arg_nr], sizeof(server_addr));
		} else {
			printf("unknown command line argument %s\nusage:\n%s\n", argv[arg_nr], help);
			return 1;
		}
	}
	printf("listen for learner on port %i, learning on server %s:%i\n", learner_port, server_addr, server_port);
	if (continuous) {
		printf("listen continuously, just kill when not needed anymore\n");
	}
	init();
	// if continuous, keep running, otherwise run once
	do {
		run();
	} while (continuous);
	
	#ifdef _WIN32
		closesocket(learner_listener_sd);
		WSACleanup();
	#elif __gnu_linux__	
		close(learner_listener_sd);	
	#endif
	return 0;
}


