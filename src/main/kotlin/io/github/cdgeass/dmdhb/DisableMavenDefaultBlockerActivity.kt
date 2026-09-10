package io.github.cdgeass.dmdhb

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.w3c.dom.CharacterData
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Disables the `maven-default-http-blocker` mirror in the bundled Maven's `settings.xml`.
 *
 * Replaces the obsolete [com.intellij.openapi.startup.StartupActivity.DumbAware] with the modern
 * [ProjectActivity] which is the recommended way since IntelliJ Platform 2023.1.
 */
class DisableMavenDefaultBlockerActivity : ProjectActivity {

    private val LOG = Logger.getInstance(DisableMavenDefaultBlockerActivity::class.java)

    override suspend fun execute(project: Project) {
        val pluginsPath = PathManager.getPreInstalledPluginsPath()
        LOG.debug("PreInstalled plugins path $pluginsPath")
        loadSettings("$pluginsPath/maven-plugin/lib/maven3/conf/settings.xml")
    }

    private fun loadSettings(path: String) {
        val dbf = DocumentBuilderFactory.newDefaultInstance()
        try {
            val db = dbf.newDocumentBuilder()
            val doc = db.parse(path)
            val mirrorsNodes = doc.getElementsByTagName("mirrors")
            if (mirrorsNodes.length == 1) {
                val mirrors = mirrorsNodes.item(0) ?: return
                val mirrorNodes = mirrors.childNodes
                for (i in 0 until mirrorNodes.length) {
                    val mirror = mirrorNodes.item(i) ?: continue
                    val childNodes = mirror.childNodes
                    for (k in 0 until childNodes.length) {
                        val childNode = childNodes.item(k) ?: continue
                        if (childNode.nodeName == "id") {
                            val data = childNode.firstChild
                            if (data is CharacterData && data.data == "maven-default-http-blocker") {
                                mirrors.removeChild(mirror)
                                val tf = TransformerFactory.newInstance()
                                val transformer = tf.newTransformer()
                                val source = DOMSource(doc)
                                val result = StreamResult(File(path))
                                transformer.transform(source, result)
                                LOG.info("Success delete the maven-default-http-blocker!")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOG.error("Delete maven-default-http-blocker error!", e)
        }
    }
}
