package horse.wtf.nzyme.dot11;

import com.codahale.metrics.MetricRegistry;
import horse.wtf.nzyme.dot11.parsers.CommunityTaggedParameters;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class Dot11TaggedParametersTest {

    /*
     * Most functionality is tested as part of higher level functionality. This test is to specifically test reported
     * issues of parsers malfunctions or unexpected payloads.
     */

    @Test
    public void testIssue666() throws Exception, MalformedFrameException {
        // Waiting for more information in issue.
        //Dot11TaggedParameters x = new Dot11TaggedParameters(new MetricRegistry(), Dot11TaggedParameters.BEACON_TAGGED_PARAMS_POSITION, CommunityTaggedParameters.ISSUE_666_PAYLOAD);
    }

}