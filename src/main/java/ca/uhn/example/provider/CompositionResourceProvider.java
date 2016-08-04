package ca.uhn.example.provider;

import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.model.primitive.UriDt;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Date;

import ca.uhn.fhir.model.dstu2.resource.Composition;


/**
 * This is a resource provider which stores Composition resources in memory using a HashMap. This is obviously not a production-ready solution for many reasons, 
 * but it is useful to help illustrate how to build a fully-functional server.
 */
public class CompositionResourceProvider implements IResourceProvider {
	
	/**
	 * This map has a resource ID as a key, and each key maps to a Deque list containing all versions of the resource with that ID.
	 */
	private Map<Long, Deque<Composition>> myIdToCompositionVersions = new HashMap<Long, Deque<Composition>>();

	/**
	 * This is used to generate new IDs
	 */
	private long myNextId = 1;
	
	/**
	 * Constructor, which pre-populates the provider with one resource instance.
	 */
	public CompositionResourceProvider() {
		long resourceId = myNextId++;
		
		Composition composition = new Composition();
		composition.setId(Long.toString(resourceId));
		IdentifierDt  identifier  = new IdentifierDt();
		identifier.setSystem(new UriDt("urn:hapitest:mrns"));
		identifier.setValue("00002");
		composition.setIdentifier(identifier);
		composition.setTitle("A Generic Diagonistic Explanatory Document");
		composition.setDateWithSecondsPrecision(new Date());
		
		//composition.addIdentifier();
		//composition.getIdentifier().get(0).setSystem(new UriDt("urn:hapitest:mrns"));
		//composition.getIdentifier().get(0).setValue("00002");
		

		LinkedList<Composition> list = new LinkedList<Composition>();
		list.add(composition);
		
		
		myIdToCompositionVersions.put(resourceId, list);

	}
	
	/**
	 * The getResourceType method comes from IResourceProvider, and must be overridden to indicate what type of resource this provider supplies.
	 */
	@Override
	public Class<Composition> getResourceType() {
		return Composition.class;
	}
	
	
	/**
	 * This is the "read" operation. The "@Read" annotation indicates that this method supports the read and/or vread operation.
	 * <p>
	 * Read operations take a single parameter annotated with the {@link IdParam} paramater, and should return a single resource instance.
	 * </p>
	 * 
	 * @param theId
	 *            The read operation takes one parameter, which must be of type IdDt and must be annotated with the "@Read.IdParam" annotation.
	 * @return Returns a resource matching this identifier, or null if none exists.
	 */
	@Read(version = true)
	public Composition readComposition(@IdParam IdDt theId) {
		Deque<Composition> retVal;
		try {
			retVal = myIdToCompositionVersions.get(theId.getIdPartAsLong());
		} catch (NumberFormatException e) {
			/*
			 * If we can't parse the ID as a long, it's not valid so this is an unknown resource
			 */
			throw new ResourceNotFoundException(theId);
		}

		if (theId.hasVersionIdPart() == false) {
			return retVal.getLast();
		} else {
			for (Composition nextVersion : retVal) {
				String nextVersionId = nextVersion.getId().getVersionIdPart();
				if (theId.getVersionIdPart().equals(nextVersionId)) {
					return nextVersion;
				}
			}
			// No matching version
			throw new ResourceNotFoundException("Unknown version: " + theId.getValue());
		}

	}
	
	@Search
	public List<Composition> findCompositionsUsingArbitraryCtriteria() {
		LinkedList<Composition> retVal = new LinkedList<Composition>();

		for (Deque<Composition> nextCompositionList : myIdToCompositionVersions.values()) {
			Composition nextComposition = nextCompositionList.getLast();
			retVal.add(nextComposition);
		}
	
		return retVal;
	}



}
