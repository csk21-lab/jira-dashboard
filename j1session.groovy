@Grab(group='com.jcraft', module='jsch', version='0.1.55') // Updated version

import com.atlassian.jira.issue.Issue
import com.atlassian.jira.component.ComponentAccessor
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
    config.put("PreferredAuthentications", "publickey,keyboard-interactive,password")
    
    JSch jsch = new JSch()
    Session session = jsch.getSession(username, server, 22)
    session.setPassword(password)
    session.setConfig(config)  // Use our custom config

    println("Connecting to server: ${server} with username: ${username}")
    session.connect()
    println("Connected successfully")

    ChannelExec channel = (ChannelExec) session.openChannel("exec")
    channel.setCommand(cmd)

    StringBuilder outputBuffer = new StringBuilder()
    StringBuilder errorBuffer = new StringBuilder()

    InputStream inputStream = channel.getInputStream()
    InputStream err = channel.getExtInputStream()

    channel.connect()

    byte[] tmp = new byte[1024]
    while (true) {
        while (inputStream.available() > 0) {
            int i = inputStream.read(tmp, 0, 1024)
            if (i < 0) break
            outputBuffer.append(new String(tmp, 0, i))
        }

        if (channel.isClosed()) {
            if ((inputStream.available() > 0) || (err.available() > 0)) continue
            println("exit-status: " + channel.getExitStatus())
            break
        }

        try {
            Thread.sleep(1000)
        } catch (Exception ee) {
            // Ignore interruptions during sleep
        }
    }

    println("output: " + outputBuffer.toString())
    println("error: " + errorBuffer.toString())

    channel.disconnect()
    session.disconnect()

} catch (Exception e) {
    e.printStackTrace()
}
