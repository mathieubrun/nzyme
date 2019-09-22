import React from 'react';
import Reflux from 'reflux';
import LoadingSpinner from "../../alerts/AlertsList";
import NetworksStore from "../../../stores/NetworksStore";
import NetworksActions from "../../../actions/NetworksActions";
import ChannelDetails from "./ChannelDetails";
import SimpleLineChart from "../../charts/SimpleLineChart";
import BeaconRate from "./BeaconRate";
import HelpBubble from "../../misc/HelpBubble";

class NetworkDetails extends Reflux.Component {

    constructor(props) {
        super(props);

        this.store = NetworksStore;

        this.stateKey = props.bssid + "_" + props.ssid;
        const state = {};
        state[this.stateKey] = undefined;
        this.state = state;
    }

    componentDidMount() {
        const bssid = this.props.bssid;
        const ssid = this.props.ssid;

        NetworksActions.findSSIDOnBSSID(bssid, ssid, true, 60*60);
        setInterval(function () {
            NetworksActions.findSSIDOnBSSID(bssid, ssid, true, 60*60)
        }, 15000);
    }

    _formatBeaconRateHistory(data) {
        const result = [];

        const avgBeaconRate = {
            x: [],
            y: [],
            type: "bar",
            name: "Beacon Rate",
            line: {width: 1, shape: "linear", color: "#2983fe"}
        };

        data.forEach(function(point) {
            const date = new Date(point["created_at"]);
            avgBeaconRate["x"].push(date);
            avgBeaconRate["y"].push(point["rate"]);
        });

        result.push(avgBeaconRate);

        return result;
    }

    render() {
        const ssid = this.state[this.stateKey];

        if (!ssid) {
            return <LoadingSpinner />;
        } else {
            return (
                <div>
                    <div className="row">
                        <div className="col-md">
                            <dl>
                                <dt>BSSID</dt>
                                <dd>{ssid.bssid}</dd>
                            </dl>
                        </div>

                        <div className="col-md-3">
                            <dl>
                                <dt>SSID</dt>
                                <dd>{ssid.name}</dd>
                            </dl>
                        </div>

                        <div className="col-md-3">
                            <dl>
                                <dt>Current Beacon Rate</dt>
                                <dd><BeaconRate rate={ssid.beacon_rate} /></dd>
                            </dl>
                        </div>
                    </div>

                    <div className="row">
                        <div className="col-md-12">
                            <hr />

                            <h3>Beacon Rate</h3>

                            <SimpleLineChart
                                title="Beacon Rate"
                                width={1100}
                                height={200}
                                customMarginLeft={60}
                                customMarginRight={60}
                                finalData={this._formatBeaconRateHistory(ssid.beacon_rate_history)}
                            />
                        </div>
                    </div>

                    <div className="row">
                        <div className="col-md-12">
                            <hr />

                            <h3>
                                Network-wide Fingerprints <small><HelpBubble link="https://go.nzyme.org/fingerprinting" /></small>
                            </h3>

                            <ul>
                                {Object.keys(ssid.fingerprints).map(function (key,i) {
                                    return <li>{ssid.fingerprints[key]}</li>
                                })}
                            </ul>
                        </div>
                    </div>

                    <div className="row">
                        <div className="col-md-12">
                            <hr />

                            <h2>Channel {this.props.channelNumber}</h2>

                            <ChannelDetails channel={ssid.channels[this.props.channelNumber]} ssid={ssid} />
                        </div>
                    </div>
                </div>
            );
        }
    }

}

export default NetworkDetails;



