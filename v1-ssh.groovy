// Import necessary libraries
@Grab(group='com.jcraft', module='jsch', version='0.1.55')
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session

// Function to execute a command on a remote server
def executeRemoteCommand(String host, String user, String password, String command) {
    JSch jsch = new JSch()
    Session session = null
    try {
        // Create a new SSH session
        session = jsch.getSession(user, host, 22)
        session.setPassword(password)

        // Configure SSH session properties
        java.util.Properties config = new java.util.Properties()
        config.put("StrictHostKeyChecking", "no")
        session.setConfig(config)

        // Connect to the remote server
        session.connect()

        // Open a channel and execute the command
        def channel = session.openChannel("exec")
        channel.setCommand(command)
        channel.setInputStream(null)
        channel.setErrStream(System.err)

        def inputStream = channel.getInputStream()
        channel.connect()

        // Read the output from the command
        def output = new StringBuilder()
        def buffer = new byte[1024]
        while (true) {
            def bytesRead = inputStream.read(buffer, 0, buffer.length)
            if (bytesRead <= 0) break
            output.append(new String(buffer, 0, bytesRead))
        }

        // Disconnect the channel and session
        channel.disconnect()
        session.disconnect()

        return output.toString()
    } catch (Exception e) {
        e.printStackTrace()
        if (session != null) {
            session.disconnect()
        }
        return null
    }
}

// Define the remote host, user, password, and command
def remoteHost = 'windows_machine_ip'
def remoteUser = 'your_windows_username'
def remotePassword = 'your_windows_password'
def curlCommand = 'curl http://example.com'

// Execute the command on the remote server
def output = executeRemoteCommand(remoteHost, remoteUser, remotePassword, curlCommand)

// Print the output
println "Output from remote command: ${output}"
