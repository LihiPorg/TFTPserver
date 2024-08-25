# TFTPserver
# Extended TFTP Server

This project provides an extended implementation of the Trivial File Transfer Protocol (TFTP) server. It supports multiple users and allows them to upload and download files, as well as receive notifications about file changes. The server and clients communicate using a binary protocol, which includes functionalities for file upload, download, and directory lookup.

## Key Features

- **User Authentication:** Log in to the server with a username.
- **File Management:** Perform file operations such as uploading, downloading, and deleting files.
- **Directory Browsing:** Request a list of all files available on the server.
- **Real-Time Notifications:** Receive notifications when files are added or removed from the server.
- **Session Management:** Disconnect from the server gracefully.

## How to Run

Follow these steps to set up and run the project:

1. **Clone the Repository:**

   ```bash
   git clone https://github.com/your-username/your-repository.git
   ```

2. **Build the Project:**

   Navigate to the project directory and build using Maven:

   ```bash
   cd your-repository
   mvn clean install
   ```

3. **Start the Server:**

   Run the server JAR file with a specified port number:

   ```bash
   java -jar server.jar <port_number>
   ```

4. **Start the Client:**

   Run the client JAR file with the server's IP address and port number:

   ```bash
   java -jar client.jar <server_ip> <server_port>
   ```

   Alternatively, use a client provided by the course staff: [TFTP Rust Client](https://github.com/bguspl/TFTP-rust-client) or create your own.

## Commands

Once the client is running, you can use the following commands to interact with the server:

- **LOGRQ:** Log in with a username.

  ```
  LOGRQ <Username>
  ```

  Example: `LOGRQ JohnDoe`

- **DELRQ:** Delete a file from the server.

  ```
  DELRQ <Filename>
  ```

  Example: `DELRQ example.txt`

- **RRQ:** Download a file from the server to your local directory.

  ```
  RRQ <Filename>
  ```

  Example: `RRQ example.txt`

- **WRQ:** Upload a file from your local directory to the server.

  ```
  WRQ <Filename>
  ```

  Example: `WRQ example.txt`

- **DIRQ:** List all files on the server.

  ```
  DIRQ
  ```

- **DISC:** Disconnect from the server.

  ```
  DISC
  ```

## Technical Details

- The server and client are both implemented in Java.
- Maven is used for building the project.
- The server uses a Thread-Per-Client model for handling connections.
- Adheres to the coding standards and guidelines specified in the assignment.

For further details on implementation, consult the assignment instructions and code documentation.
