package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.IReportCreator
import groovy.transform.CompileStatic
import groovy.transform.ToString
import spock.lang.Specification
import spock.lang.Unroll

/**
 *
 * User: Renato
 */
class ConfigLoaderSpec extends Specification {

    private static final String FEATURE_REPORT_CSS = HtmlReportCreator.class.name + '.featureReportCss'

    static final String PROP_HIDE_EMPTY_BLOCKS = 'com.athaydes.spockframework.report.hideEmptyBlocks'

    def "The ConfigLoader should load the default configurations"() {
        given:
        "A ConfigLoader without any custom configuration"
        def configLoader = new ConfigLoader()

        and:
        "The configLocation exists"
        ( ConfigLoader.CUSTOM_CONFIG as File ).exists()

        when:
        "I ask the ConfigLoader to load the configuration"
        def result = configLoader.loadConfig()

        then:
        "The ConfigLoader to find all of the properties declared in the configLocation"
        result.getProperty( FEATURE_REPORT_CSS ) == 'spock-feature-report.css'
        result.getProperty( 'com.athaydes.spockframework.report.hideEmptyBlocks' ) == 'false'
    }

    def "Custom configurations should override default configurations"() {
        given:
        "A ConfigLoader in an environment where there is a custom config file"
        def configLoader = new ConfigLoader()
        File customFile = createFileUnderMetaInf( IReportCreator.class.name + '.properties' )
        customFile.write "${FEATURE_REPORT_CSS}=${expected}"

        and:
        "The configLocation exists"
        assert customFile.exists()

        when:
        "I ask the ConfigLoader to load the configuration"
        def result = configLoader.loadConfig()

        then:
        "The ConfigLoader to find all of the properties declared in the configLocation"
        result.getProperty( FEATURE_REPORT_CSS ) == expected

        and:
        "The default properties are also kept"
        result.getProperty( 'com.athaydes.spockframework.report.hideEmptyBlocks' ) == 'false'
        result.getProperty( 'com.athaydes.spockframework.report.outputDir' ) == 'build/spock-reports'

        cleanup:
        assert customFile.delete()

        where:
        expected << [ 'example/report.css' ]
    }

    def 'System properties should override props files'() {
        given:
        "A ConfigLoader without any custom configuration"
        def configLoader = new ConfigLoader()

        and:
        "The configLocation exists"
        ( ConfigLoader.CUSTOM_CONFIG as File ).exists()

        and:
        "I have specified a system property override"
        def origPropVal = System.properties[ PROP_HIDE_EMPTY_BLOCKS ]
        System.properties[ PROP_HIDE_EMPTY_BLOCKS ] = expected

        when:
        "I ask the ConfigLoader to load the configuration"
        def result = configLoader.loadConfig()

        then:
        "The ConfigLoader must use the value from the system property override"
        result.getProperty( PROP_HIDE_EMPTY_BLOCKS ) == expected

        cleanup:
        if ( origPropVal )
            System.properties[ PROP_HIDE_EMPTY_BLOCKS ] = origPropVal
        else
            System.properties.remove PROP_HIDE_EMPTY_BLOCKS

        where:
        expected = 'custom_value'
    }

    @Unroll
    def "ConfigLoader can apply properties with the correct types"() {
        given:
        "A ConfigLoader without any custom configuration"
        def configLoader = new ConfigLoader()

        and:
        'Properties of several different types'
        def properties = new Properties()
        ( propertiesMap + methodCalls ).each { k, v ->
            properties[ propertyPrefix + k ] = v.toString() // all properties come in Stringified
        }

        when: 'The ReportCreatorWithManyProperties instance is populated with the properties'
        def reporter = new ReportCreatorWithManyProperties()
        configLoader.apply( reporter, properties )

        then: 'All properties of the report creator are set as expected'
        reporter.cool == propertiesMap.cool
        reporter.name == propertiesMap.name
        reporter.count == propertiesMap.count

        //noinspection GrEqualsBetweenInconvertibleTypes
        reporter.percentage == propertiesMap.percentage

        and: 'All method setters are called as expected'
        reporter.methodCalls == methodCalls

        where:
        propertiesMap << [
                [ cool: true, name: 'nice', count: 4, percentage: 0.33 ],
                [ cool: false, name: 'boo', count: -2, percentage: -0.1 ],
                [ cool: true, name: 'nice', count: 4, percentage: 0.33 ],
                [ cool: false, name: 'boo', count: -2, percentage: -0.1 ],
        ]

        propertyPrefix << [
                IReportCreator.package.name + '.',
                IReportCreator.package.name + '.',
                ReportCreatorWithManyProperties.name + '.',
                ReportCreatorWithManyProperties.name + '.'
        ]

        methodCalls << [
                [ : ],
                [ outputDir: 'hi', projectName: 'hello', projectVersion: '1.0' ],
                [ hideEmptyBlocks: true, showCodeBlocks: true ],
                [ showCodeBlocks: false ],
        ]

    }

    private createFileUnderMetaInf( String fileName ) {
        def globalExtConfig = this.class.getResource( '/META-INF/services/org.spockframework.runtime.extension.IGlobalExtension' )
        def f = new File( globalExtConfig.toURI() )
        new File( f.parentFile, fileName )
    }

}

@CompileStatic
@ToString
class ReportCreatorWithManyProperties implements IReportCreator {

    boolean cool
    String name
    int count
    double percentage

    final Map methodCalls = [ : ]

    @Override
    void createReportFor( SpecData data ) {}

    @Override
    void setOutputDir( String path ) {
        methodCalls << [ 'outputDir': path ]
    }

    @Override
    void setHideEmptyBlocks( boolean hide ) {
        methodCalls << [ 'hideEmptyBlocks': hide ]
    }

    @Override
    void setShowCodeBlocks( boolean show ) {
        methodCalls << [ 'showCodeBlocks': show ]

    }

    @Override
    void setTestSourceRoots( String roots ) {
        methodCalls << [ 'testSourceRoots': roots ]

    }

    @Override
    void setProjectName( String projectName ) {
        methodCalls << [ 'projectName': projectName ]

    }

    @Override
    void setProjectVersion( String projectVersion ) {
        methodCalls << [ 'projectVersion': projectVersion ]
    }

    @Override
    void done() {}
}