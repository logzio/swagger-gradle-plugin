package com.benjaminsproule.swagger.gradleplugin

import com.benjaminsproule.swagger.gradleplugin.model.SwaggerExtension
import groovy.json.JsonSlurper
import org.gradle.api.Project
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test

import java.nio.file.Files

class SpringMvcPluginTest {
    Project project

    @Before
    void setUp() {
        project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'com.benjaminsproule.swagger'
    }

    @After
    void tearDown() {
        project = null
    }

    @Test
    void producesSwaggerDocumentation() {
        project.configurations.create('runtime')
        project.plugins.apply JavaPlugin


        def expectedSwaggerDirectory = "${project.buildDir}/swaggerui-" + UUID.randomUUID()
        project.extensions.configure(SwaggerExtension, new ClosureBackedAction<SwaggerExtension>(
            {
                apiSource {
                    locations = ['com.benjaminsproule']
                    springmvc = true
                    schemes = ['http']
                    info {
                        title = project.name
                        version = '1'
                        license {
                            name = 'Apache 2.0'
                        }
                        contact {
                            name = 'Joe Blogs'
                        }
                    }
                    swaggerDirectory = expectedSwaggerDirectory
                    host = 'localhost:8080'
                    basePath = '/'
                    securityDefinition {
                        name = 'MyBasicAuth'
                        type = 'basic'
                    }
                }
            }
        ))

        project.tasks.generateSwaggerDocumentation.execute()

        def swaggerFile = new File("${expectedSwaggerDirectory}/swagger.json")
        assertSwaggerJson(swaggerFile)
    }

    private static void assertSwaggerJson(File swaggerJsonFile) {
        assert Files.exists(swaggerJsonFile.toPath())

        JsonSlurper jsonSlurper = new JsonSlurper()

        def producedSwaggerDocument = jsonSlurper.parse(swaggerJsonFile)

        assert producedSwaggerDocument.get('swagger') == '2.0'
        assert producedSwaggerDocument.get('host') == 'localhost:8080'
        assert producedSwaggerDocument.get('basePath') == '/'

        def info = producedSwaggerDocument.get('info')
        assert info
        assert info.get('version') == '1'
        assert info.get('title') == 'test'

        def tags = producedSwaggerDocument.get('tags')
        assert tags
        assert tags.size() == 1
        assert tags.get(0).get('name') == 'Test'

        def paths = producedSwaggerDocument.get('paths')
        assert paths
        assert paths.size() == 4
        assert paths.get('/root/withannotation/basic')
        assert paths.get('/root/withannotation/basic').get('get')
        assert paths.get('/root/withannotation/generics')
        assert paths.get('/root/withannotation/generics').get('post')
        assert paths.get('/root/withoutannotation/basic')
        assert paths.get('/root/withoutannotation/basic').get('get')
        assert paths.get('/root/withoutannotation/generics')
        assert paths.get('/root/withoutannotation/generics').get('post')
    }
}
