/*
 * This file is part of nzyme.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */

package horse.wtf.nzyme.rest.resources;

import com.google.common.collect.Lists;
import horse.wtf.nzyme.NzymeLeader;
import horse.wtf.nzyme.bandits.Bandit;
import horse.wtf.nzyme.bandits.Contact;
import horse.wtf.nzyme.bandits.identifiers.BanditIdentifier;
import horse.wtf.nzyme.bandits.trackers.Tracker;
import horse.wtf.nzyme.bandits.trackers.TrackerManager;
import horse.wtf.nzyme.bandits.trackers.protobuf.TrackerMessage;
import horse.wtf.nzyme.rest.authentication.Secured;
import horse.wtf.nzyme.rest.requests.BanditTrackRequest;
import horse.wtf.nzyme.rest.responses.bandits.BanditResponse;
import horse.wtf.nzyme.rest.responses.bandits.ContactResponse;
import horse.wtf.nzyme.rest.responses.trackers.TrackerResponse;
import horse.wtf.nzyme.rest.responses.trackers.TrackersListResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Path("/api/trackers")
@Secured
@Produces(MediaType.APPLICATION_JSON)
public class TrackersResource {

    private static final Logger LOG = LogManager.getLogger(TrackersResource.class);

    @Inject
    private NzymeLeader nzyme;

    @GET
    public Response findAll() {
        List<TrackerResponse> trackers = Lists.newArrayList();
        for (Tracker tracker : nzyme.getTrackerManager().getTrackers().values()) {
            trackers.add(TrackerResponse.create(
                    tracker.getName(),
                    tracker.getVersion(),
                    tracker.getLastSeen(),
                    TrackerManager.decideTrackerState(tracker),
                    tracker.getTrackingMode(),
                    buildContactList(nzyme.getContactManager().findContactsOfTracker(tracker)),
                    nzyme.getGroundStation().trackerHasPendingAnyTrackingRequest(tracker.getName()),
                    tracker.getRssi()
            ));
        }

        return Response.ok(TrackersListResponse.create(
                nzyme.getGroundStation() != null,
                trackers.size(),
                trackers
        )).build();
    }

    @GET
    @Path("/show/{name}")
    public Response findTracker(@PathParam("name") String name) {
        Map<String, Tracker> trackers = nzyme.getTrackerManager().getTrackers();
        if (trackers.containsKey(name)) {
            Tracker tracker = trackers.get(name);

            return Response.ok(TrackerResponse.create(
                    tracker.getName(),
                    tracker.getVersion(),
                    tracker.getLastSeen(),
                    TrackerManager.decideTrackerState(tracker),
                    tracker.getTrackingMode(),
                    buildContactList(nzyme.getContactManager().findContactsOfTracker(tracker)),
                    nzyme.getGroundStation().trackerHasPendingAnyTrackingRequest(tracker.getName()),
                    tracker.getRssi()
            )).build();
        } else {
            LOG.info("Tracker [{}] not found.", name);
            return Response.status(404).build();
        }
    }

    private List<ContactResponse> buildContactList(List<Contact> contacts) {
        List<ContactResponse> result = Lists.newArrayList();
        for (Contact contact : contacts) {
            Optional<Bandit> opt = nzyme.getContactManager().findBanditByDatabaseId(contact.banditId());
            if (opt.isEmpty()) {
                continue;
            }
            Bandit bandit = opt.get();

            result.add(ContactResponse.create(
                    contact.uuid(),
                    contact.frameCount(),
                    contact.firstSeen(),
                    contact.lastSeen(),
                    contact.isActive(),
                    contact.lastSignal(),
                    bandit.uuid().toString(),
                    bandit.name(),
                    contact.sourceRole().toString(),
                    contact.sourceName(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
        }

        return result;
    }

    @POST
    @Path("/show/{name}/command/start_track_request")
    public Response issueStartTrackRequest(@PathParam("name") String trackerName, BanditTrackRequest trackRequest) {
        // Check if tracker exists.
        if (!nzyme.getTrackerManager().getTrackers().containsKey(trackerName)) {
            LOG.warn("Tracker not found.");
            return Response.status(404).build();
        }

        // Check if bandit exists.
        Bandit bandit = nzyme.getContactManager().getBandits().get(trackRequest.banditUUID());
        if (bandit == null) {
            LOG.warn("Bandit not found.");
            return Response.status(404).build();
        }

        TrackerMessage.StartTrackRequest.Builder builder = TrackerMessage.StartTrackRequest.newBuilder()
                .setSource(nzyme.getNodeID())
                .setReceiver(trackerName)
                .setUuid(bandit.uuid().toString());

        try {
            if (bandit.identifiers() != null) {
                for (BanditIdentifier identifier : bandit.identifiers()) {
                    TrackerMessage.ContactIdentifier.Builder idBuilder = TrackerMessage.ContactIdentifier.newBuilder()
                            .setType(identifier.getType().toString())
                            .setUuid(identifier.getUuid().toString());

                    for (Map.Entry<String, Object> config : identifier.configuration().entrySet()) {
                        Object value;
                        if (config.getValue() instanceof List) {
                            value = "nzl:" + nzyme.getObjectMapper().writeValueAsString(config.getValue());
                        } else if (config.getValue() instanceof Map) {
                            value = "nzm:" + nzyme.getObjectMapper().writeValueAsString(config.getValue());
                        } else {
                            value = config.getValue();
                        }
                        String configRep = config.getKey() + ":\"" + value + "\"";
                        idBuilder.addConfiguration(configRep);
                    }

                    builder.addIdentifier(idBuilder.build());
                }
            }
        } catch(Exception e) {
            LOG.error("Could not build bandit identifiers.", e);
            return Response.status(500).build();
        }

        nzyme.getGroundStation().startTrackRequest(builder.build());
        return Response.accepted().build();
    }

    @POST
    @Path("/show/{name}/command/cancel_track_request")
    public Response issueCancelTrackRequest(@PathParam("name") String trackerName, BanditTrackRequest trackRequest) {
        // Check if tracker exists.
        if (!nzyme.getTrackerManager().getTrackers().containsKey(trackerName)) {
            return Response.status(404).build();
        }

        nzyme.getGroundStation().cancelTrackRequest(TrackerMessage.CancelTrackRequest.newBuilder()
                .setSource(nzyme.getNodeID())
                .setReceiver(trackerName)
                .build());

        return Response.accepted().build();
    }

}
