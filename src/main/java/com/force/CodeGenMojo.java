package com.force;

import java.io.File;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import com.force.sdk.codegen.ForceJPAClassGenerator;
import com.force.sdk.codegen.filter.FieldCombinationFilter;
import com.force.sdk.codegen.filter.FieldReferenceFilter;
import com.force.sdk.codegen.filter.ObjectCombinationFilter;
import com.force.sdk.codegen.filter.ObjectNameFilter;
import com.force.sdk.codegen.filter.ObjectNameWithRefFilter;
import com.force.sdk.codegen.filter.ObjectNoOpFilter;
import com.force.sdk.connector.ForceServiceConnector;
import com.sforce.soap.partner.PartnerConnection;

/**
 * Goal which generates Java Classes based on Force.com objects.
 * 
 * @goal codegen
 * @phase generate-sources
 * 
 * @author Tim Kral
 */
public class CodeGenMojo extends AbstractMojo {
    
    /**
     * Named configuration for connecting to Force.com.
     * @parameter expression="${force.codegen.connectionName}"
     * @required
     */
    private String connectionName;
    
    /**
     * Use all Force.com objects for generation.
     * @parameter expression="${force.codegen.all}" default-value=false
     */
    private boolean all;
    
    /**
     * Names of Force.com objects to include for generation.
     * @parameter
     */
    private Set<String> includes;
    
    /**
     * Names of Force.com objects to exclude for generation.
     * @parameter
     */
    private Set<String> excludes;
    
    /**
     * Whether to include Force.com object references.
     * @parameter expression="${force.codegen.followReferences}" default-value=true
     */
    private boolean followReferences;
    
    /**
     * Java package name for generated code.
     * @parameter expression="${force.codegen.packageName}"
     */
    private String packageName;
    
    /**
     * Destination directory for generated code.
     * @parameter expression="${force.codegen.destDir}" default-value="${project.build.directory}/generated-sources"
     */
    private File destDir;
    
    /**
     * Whether to skip codegen execution.
     * @parameter expression="${skipForceCodeGen}" default-value=false
     */
    private boolean skip;
    
    public void execute() throws MojoExecutionException {
        
    	if (skip) {
            getLog().info("Skipping Force.com code generation.");
            return;
    	}
        
        PartnerConnection conn;
        try {
            ForceServiceConnector connector = getConnector();
            getLog().debug("Establishing connection to Force.com");
            conn = connector.getConnection();
        } catch (MojoExecutionException mee) {
            throw mee;
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to establish connection to Force.com", e);
        }
        
        ForceJPAClassGenerator generator = new ForceJPAClassGenerator();
        generator.setPackageName(packageName);
        if (!initFilters(generator)) return;
        
        int numGeneratedFiles;
        try {
        	getLog().info("Generating Force.com JPA classes in " + destDir);
            numGeneratedFiles = generator.generateCode(conn, destDir);
        } catch (Exception e) {
        	getLog().error("Unable to generate JPA classes", e);
            return;
        }
        
        getLog().info("Successfully generated " + numGeneratedFiles + " JPA classes");
    }
    
    ForceServiceConnector getConnector() throws MojoExecutionException {
        getLog().debug("Initializing connection to Force.com using named configuration '" + connectionName + "'");
        return new ForceServiceConnector(connectionName);
    }
    
    boolean initFilters(ForceJPAClassGenerator generator) {
        
        getLog().debug("Setting up code generation filters");
        if (all) {
            getLog().debug("Filtering in all Force.com objects");
            generator.setObjectFilter(new ObjectNoOpFilter());
        } else {
            ObjectCombinationFilter objectFilter = new ObjectCombinationFilter();
            FieldCombinationFilter fieldFilter = new FieldCombinationFilter();
            
            if (includes != null && !includes.isEmpty()) {
                if (followReferences) {
                    getLog().debug("Filtering in the following Force.com objects with references " + includes);
                    objectFilter.addFilter(new ObjectNameWithRefFilter(includes));
                } else {
                    getLog().debug("Filtering in the following Force.com objects without references " + includes);
                    objectFilter.addFilter(new ObjectNameFilter(true, includes));
                    fieldFilter.addFilter(new FieldReferenceFilter(true, includes));
                }
            }
            
            if (excludes != null && !excludes.isEmpty()) {
                getLog().debug("Filtering out the following Force.com objects " + excludes);
                objectFilter.addFilter(new ObjectNameFilter(false, excludes));
                fieldFilter.addFilter(new FieldReferenceFilter(false, excludes));
            }
            
            if (objectFilter.getFilterList().isEmpty()) {
                getLog().warn("No JPA classes generated. Please specify the schema object names or use -Dforce.codegen.all");
                return false;
            }
            
            generator.setObjectFilter(objectFilter);
            if (!fieldFilter.getFilterList().isEmpty()) {
                generator.setFieldFilter(fieldFilter);
            }
        }
        
        return true;
    }
}
