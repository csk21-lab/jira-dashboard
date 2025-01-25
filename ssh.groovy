@Grab(group='com.jcraft', module='jsch', version='0.1.46')

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.ChannelExec
import java.io.InputStream

def cmd = 'curl -X "GET" "https://xyz.com/e/8094d260-d7eb-41e9-a0a4-42ae7ffc5cb3/api/v1/deployment/installer/agent/windows/default/latest?flavor=default&arch=all&bitness=all&skipMetadata=false" -H "accept: application/octet-stream" -H "Authorization: Api-Token dtabc" --insecure --output "C:/Users/Windows-latest.exe"'
def server = 'ABC.COM'
def username = 'username'
def password = 'password'
def port = 11

try {
    // Setup SSH session
    java.util.Properties config = new java.util.Properties()
    JSch jsch = new JSch()
    Session session = jsch.getSession(username, server, port)
    session.setPassword(password)
    session.setConfig("StrictHostKeyChecking", "no")
    session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password")
    session.connect()

    // Create channel for execution
    ChannelExec channel = (ChannelExec) session.openChannel("exec")
    channel.setCommand(cmd)

    // Buffers for output and error streams
    StringBuilder outputBuffer = new StringBuilder()
    StringBuilder errorBuffer = new StringBuilder()

    // Input and error streams
    InputStream inputStream = channel.getInputStream()
    InputStream errStream = channel.getExtInputStream()

    // Connect channel and execute the command
    channel.connect()

    byte[] tmp = new byte[1024]
    while (true) {
        // Capture command output
        while (inputStream.available() > 0) {
            int i = inputStream.read(tmp, 0, 1024)
            if (i < 0) break
            outputBuffer.append(new String(tmp, 0, i))
        }

        // Capture error output
        while (errStream.available() > 0) {
            int i = errStream.read(tmp, 0, 1024)
            if (i < 0) break
            errorBuffer.append(new String(tmp, 0, i))
        }

        // Exit condition
        if (channel.isClosed()) {
            if ((inputStream.available() > 0) || (errStream.available() > 0)) continue
            int exitStatus = channel.getExitStatus()
            if (exitStatus != 0) {
                // Log failure if exit status is non-zero
                System.out.println("Curl command failed with exit status: " + exitStatus)
                System.out.println("Error: " + errorBuffer.toString())
            } else {
                // Log success if exit status is zero
                System.out.println("Curl command executed successfully")
                System.out.println("Output: " + outputBuffer.toString())
            }
            break
        }

        try {
            Thread.sleep(1000)
        } catch (Exception ee) {}
    }

    // Disconnect channel and session
    channel.disconnect()
    session.disconnect()

} catch (Exception e) {
    // Log any exception during execution
    System.out.println("Error occurred while executing command:")
    e.printStackTrace()
}



//def cmd = 'curl -v -X "GET" "https://xyz.com/e/8094d260-d7eb-41e9-a0a4-42ae7ffc5cb3/api/v1/deployment/installer/agent/windows/default/latest?flavor=default&arch=all&bitness=all&skipMetadata=false" -H "accept: application/octet-stream" -H "Authorization: Api-Token dtabc" --insecure --output "C:/Users/Windows-latest.exe"'

//def cmd = 'curl -X "GET" "https://xyz.com/e/8094d260-d7eb-41e9-a0a4-42ae7ffc5cb3/api/v1/deployment/installer/agent/windows/default/latest?flavor=default&arch=all&bitness=all&skipMetadata=false" -H "accept: application/octet-stream" -H "Authorization: Api-Token dtabc" --insecure --output "C:/Users/Windows-latest.exe" --max-time 300'
