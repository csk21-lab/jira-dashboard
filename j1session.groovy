@Grab(group='com.jcraft', module='jsch', version='0.1.55') // Ensure you're using the latest version

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.ChannelExec

def cmd = 'cd /home/abcd \n cat hello.txt'
def server = 'sample.com'
def username = 'abcd'
def password = 'xyz'

try {
    // Set up the configuration properties for the SSH session
    def config = new Properties()
    config.put("StrictHostKeyChecking", "no")
    
    JSch jsch = new JSch()
    Session session = jsch.getSession(username, server, 22)
    session.setPassword(password)
    session.setConfig(config)  // Use the custom config

    println("Attempting to connect to server: ${server} with username: ${username}")
    session.connect()
    println("Connection established successfully")

    ChannelExec channel = (ChannelExec) session.openChannel("exec")
    channel.setCommand(cmd)

    StringBuilder outputBuffer = new StringBuilder()
    StringBuilder errorBuffer = new StringBuilder()

    InputStream inputStream = channel.getInputStream()
    InputStream errStream = channel.getErrStream()

    channel.connect()
    println("Channel connected, executing command...")

    byte[] tmp = new byte[1024]
    while (true) {
        while (inputStream.available() > 0) {
            int i = inputStream.read(tmp, 0, 1024)
            if (i < 0) break
            outputBuffer.append(new String(tmp, 0, i))
        }

        while (errStream.available() > 0) {
            int i = errStream.read(tmp, 0, 1024)
            if (i < 0) break
            errorBuffer.append(new String(tmp, 0, i))
        }

        if (channel.isClosed()) {
            if ((inputStream.available() > 0) || (errStream.available() > 0)) continue
            println("exit-status: " + channel.getExitStatus())
            break
        }

        try {
            Thread.sleep(1000)
        } catch (Exception e) {
            // Ignore interruptions during sleep
        }
    }

    println("Command output: " + outputBuffer.toString())
    println("Command error: " + errorBuffer.toString())

    channel.disconnect()
    session.disconnect()
    println("Session disconnected successfully")

} catch (Exception e) {
    e.printStackTrace()
    println("An error occurred: " + e.message)
}
