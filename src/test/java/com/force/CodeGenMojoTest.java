/**
 * Copyright (c) 2011, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.force;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.After;
import org.junit.Test;

import com.force.sdk.codegen.ForceJPAClassGenerator;
import com.force.sdk.codegen.filter.FieldCombinationFilter;
import com.force.sdk.codegen.filter.FieldFilter;
import com.force.sdk.codegen.filter.FieldReferenceFilter;
import com.force.sdk.codegen.filter.ForceJPAFieldFilter;
import com.force.sdk.codegen.filter.ObjectCombinationFilter;
import com.force.sdk.codegen.filter.ObjectFilter;
import com.force.sdk.codegen.filter.ObjectNameFilter;
import com.force.sdk.codegen.filter.ObjectNameWithRefFilter;
import com.force.sdk.codegen.filter.ObjectNoOpFilter;
import com.force.sdk.connector.ForceServiceConnector;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Basic tests for {@code CodeGenMojo}.
 * 
 * @author Tim Kral
 */
public class CodeGenMojoTest extends AbstractMojoTestCase {

	static final String POM_ROOT_DIR = "src/test/resources/poms/";
	
	@Override
	protected void setUp() throws Exception {
		// This is required
		super.setUp();
	}
	
	@Override
	protected void tearDown() throws Exception {
		// This is required
		super.tearDown();
	}
	
	@After
	public void deleteDestinationDirectory() {
		new File(POM_ROOT_DIR + "target").delete();
	}
	
	@Test
	public void testTestEnvironment() throws Exception {
		loadCodeGenMojo("BasicPom.xml");
	}
	
	@Test
	public void testGetConnection() throws Exception {
		CodeGenMojo codeGenMojo = loadCodeGenMojo("BasicPom.xml");
		
		ForceServiceConnector connector = codeGenMojo.getConnector();
		connector.getConnection();
	}
	
	@Test
	public void testNoObjectsSpecified() throws Exception {
	    CodeGenMojo codeGenMojo = loadCodeGenMojo("BasicPom.xml");
	    
	    ForceJPAClassGenerator generator = new ForceJPAClassGenerator();
	    assertFalse("Unexpected result for initFilters", codeGenMojo.initFilters(generator));
	}
	
	@Test
	public void testAll() throws Exception {
		CodeGenMojo codeGenMojo = loadCodeGenMojo("AllPom.xml");
		
		ForceJPAClassGenerator generator = new ForceJPAClassGenerator();
		assertTrue("Unexpected result for initFilters", codeGenMojo.initFilters(generator));
		
		assertEquals("Unexpected object filter", ObjectNoOpFilter.class, generator.getObjectFilter().getClass());
		
		// By default the JPA generator uses this field filter
		assertEquals("Unexpected field filter", ForceJPAFieldFilter.class, generator.getFieldFilter().getClass());
	}
	
	@Test
	public void testExcludes() throws Exception {
	    CodeGenMojo codeGenMojo = loadCodeGenMojo("ExcludesPom.xml");
	    
	    ForceJPAClassGenerator generator = new ForceJPAClassGenerator();
	    assertTrue("Unexpected result for initFilters", codeGenMojo.initFilters(generator));
	    
	    assertObjectCombinationFilter(generator.getObjectFilter(), ObjectNameFilter.class);
	    assertObjectNameFilter(retrieveObjectFilter(generator.getObjectFilter(), 0), false /*include*/,
	            ImmutableSet.<String>of("Account"));
	    
	    assertFieldCombinationFilter(generator.getFieldFilter(), FieldCombinationFilter.class, ForceJPAFieldFilter.class);
	    
	    // We should have a combination filter embedded within a combination filter
        FieldFilter embeddedComboFilter = ((FieldCombinationFilter) generator.getFieldFilter()).getFilterList().get(0);
        assertFieldCombinationFilter(embeddedComboFilter, FieldReferenceFilter.class);
        assertFieldReferenceFilter(((FieldCombinationFilter) embeddedComboFilter).getFilterList().get(0), false /*include*/,
                ImmutableSet.<String>of("Account"));
	}
	
	@Test
	public void testIncludesExcludes() throws Exception {
	    CodeGenMojo codeGenMojo = loadCodeGenMojo("IncludesExcludesPom.xml");
	    
	    ForceJPAClassGenerator generator = new ForceJPAClassGenerator();
	    assertTrue("Unexpected result for initFilters", codeGenMojo.initFilters(generator));
	    
	    assertObjectCombinationFilter(generator.getObjectFilter(), ObjectNameWithRefFilter.class, ObjectNameFilter.class);
	    assertObjectNameFilter(retrieveObjectFilter(generator.getObjectFilter(), 1), false /*include*/,
	            ImmutableSet.<String>of("Contact"));
	    
	    assertFieldCombinationFilter(generator.getFieldFilter(), FieldCombinationFilter.class, ForceJPAFieldFilter.class);
        
        // We should have a combination filter embedded within a combination filter
        FieldFilter embeddedComboFilter = ((FieldCombinationFilter) generator.getFieldFilter()).getFilterList().get(0);
        assertFieldCombinationFilter(embeddedComboFilter, FieldReferenceFilter.class);
        assertFieldReferenceFilter(((FieldCombinationFilter) embeddedComboFilter).getFilterList().get(0), false /*include*/,
                ImmutableSet.<String>of("Contact"));
	}
	
	@Test
    public void testIncludesWithoutRef() throws Exception {
        CodeGenMojo codeGenMojo = loadCodeGenMojo("IncludesWithoutRefPom.xml");
            
        ForceJPAClassGenerator generator = new ForceJPAClassGenerator();
        assertTrue("Unexpected result for initFilters", codeGenMojo.initFilters(generator));
        
        assertObjectCombinationFilter(generator.getObjectFilter(), ObjectNameFilter.class);
        assertObjectNameFilter(retrieveObjectFilter(generator.getObjectFilter(), 0), true /*include*/,
                ImmutableSet.<String>of("Account", "Opportunity"));
        
        assertFieldCombinationFilter(generator.getFieldFilter(), FieldCombinationFilter.class, ForceJPAFieldFilter.class);
        
        // We should have a combination filter embedded within a combination filter
        FieldFilter embeddedComboFilter = ((FieldCombinationFilter) generator.getFieldFilter()).getFilterList().get(0);
        assertFieldCombinationFilter(embeddedComboFilter, FieldReferenceFilter.class);
        assertFieldReferenceFilter(((FieldCombinationFilter) embeddedComboFilter).getFilterList().get(0), true /*include*/,
                ImmutableSet.<String>of("Account", "Opportunity"));
        
    }

    @Test
	public void testIncludesWithRef() throws Exception {
	    CodeGenMojo codeGenMojo = loadCodeGenMojo("IncludesWithRefPom.xml");
	        
        ForceJPAClassGenerator generator = new ForceJPAClassGenerator();
        assertTrue("Unexpected result for initFilters", codeGenMojo.initFilters(generator));
        
        // We should only run the ObjectNameWithRefFilter
        assertObjectCombinationFilter(generator.getObjectFilter(), ObjectNameWithRefFilter.class);
        
        // By default the JPA generator uses this field filter
        assertEquals("Unexpected field filter", ForceJPAFieldFilter.class, generator.getFieldFilter().getClass());
	}
	
    private CodeGenMojo loadCodeGenMojo(String pom) throws Exception {
		File testPom = getTestFile(POM_ROOT_DIR + pom);
		
		CodeGenMojo codeGenMojo = (CodeGenMojo) lookupMojo("codegen", testPom);
		assertNotNull("Could not load " + CodeGenMojo.class.getSimpleName() + " from " + POM_ROOT_DIR + pom, codeGenMojo);
		return codeGenMojo;
	}
	
	private void assertObjectCombinationFilter(ObjectFilter objectFilter, Class<?>...objectFilterClasses) {
	    assertEquals("Unexpected object filter", ObjectCombinationFilter.class, objectFilter.getClass());
	    List<ObjectFilter> objectFilterList = ((ObjectCombinationFilter) objectFilter).getFilterList();
        assertEquals("Unexpected number of object filters", objectFilterClasses.length, objectFilterList.size());
        
        for (int i = 0; i < objectFilterClasses.length; i++) {
            assertEquals("Unexpected object filter in filter list", objectFilterClasses[i], objectFilterList.get(i).getClass());
        }
	}
	
	private void assertObjectNameFilter(ObjectFilter objectFilter, boolean expectedInclude,
	        Set<String> expectedObjectNames) {
	    assertEquals("Unexpected object filter", ObjectNameFilter.class, objectFilter.getClass());
        
	    ObjectNameFilter objectNameFilter = (ObjectNameFilter) objectFilter;
        assertEquals("Unexpected include state", expectedInclude, objectNameFilter.isInclude());
        
        Set<String> setDiff = Sets.difference(objectNameFilter.getObjectNames(), expectedObjectNames);
        assertTrue("Found difference in filtered object names " + setDiff, setDiff.isEmpty());
	}
	
	private void assertFieldCombinationFilter(FieldFilter fieldFilter, Class<?>...fieldFilterClasses) {
        assertEquals("Unexpected field filter", FieldCombinationFilter.class, fieldFilter.getClass());
        List<FieldFilter> fieldFilterList = ((FieldCombinationFilter) fieldFilter).getFilterList();
        assertEquals("Unexpected number of field filters", fieldFilterClasses.length, fieldFilterList.size());
        
        for (int i = 0; i < fieldFilterClasses.length; i++) {
            assertEquals("Unexpected field filter in filter list", fieldFilterClasses[i], fieldFilterList.get(i).getClass());
        }
    }
	
	private void assertFieldReferenceFilter(FieldFilter fieldFilter, boolean expectedInclude,
	        Set<String> expectedReferenceObjectNames) {
	    assertEquals("Unexpected field filter", FieldReferenceFilter.class, fieldFilter.getClass());
	    
	    FieldReferenceFilter fieldRefFilter = (FieldReferenceFilter) fieldFilter;
	    assertEquals("Unexpected include state", expectedInclude, fieldRefFilter.isInclude());
	    
	    Set<String> setDiff = Sets.difference(fieldRefFilter.getReferenceObjectNames(), expectedReferenceObjectNames);
	    assertTrue("Found difference in filtered reference object names " + setDiff, setDiff.isEmpty());
	}
	
	private ObjectFilter retrieveObjectFilter(ObjectFilter objectFilter, int index) {
	    return ((ObjectCombinationFilter) objectFilter).getFilterList().get(index);
	}
}
