import React from 'react';
import Reflux from 'reflux';

import {
    BrowserRouter as Router,
    Switch,
    Route
} from 'react-router-dom';

import Notifications from 'react-notify-toast';

import PingStore from "./stores/PingStore";
import PingActions from "./actions/PingActions";

import NavigationBar from './components/layout/NavigationBar';
import OverviewPage from "./components/overview/OverviewPage";
import NotConnectedPage from "./components/misc/NotConnectedPage";
import NotFoundPage from "./components/misc/NotFoundPage";
import AlertDetailsPage from "./components/alerts/AlertDetailsPage";
import Routes from "./util/Routes";
import Footer from "./components/layout/Footer";
import SystemPage from "./components/system/SystemPage";

class App extends Reflux.Component {

    constructor(props) {
        super(props);

        this.state = {
            apiConnected: true
        };

        this.store = PingStore;
    }

    componentDidMount() {
        PingActions.ping();
    }

    render() {
        if(this.state.apiConnected) {
            return (
                <Router>
                    <div className="nzyme">
                        <NavigationBar/>

                        <div className="container">
                            <Notifications/>

                            <Switch>
                                <Route exact path={Routes.DASHBOARD} component={OverviewPage} />

                                { /* Alerts. */ }
                                <Route exact path={Routes.SYSTEM_STATUS} component={SystemPage} />

                                { /* Alerts. */ }
                                <Route path={Routes.ALERTS.SHOW(":id")} component={AlertDetailsPage} />

                                { /* 404. */ }
                                <Route path={Routes.NOT_FOUND} component={NotFoundPage} />
                                <Route path="*" component={NotFoundPage} /> { /* Catch-all.  */ }
                            </Switch>

                            <Footer/>
                        </div>
                    </div>
                </Router>
            );
        } else {
            return (
                <div className="nzyme">
                    <NavigationBar/>

                    <div className="container">
                        <Notifications />
                        <NotConnectedPage />
                        <Footer/>
                    </div>
                </div>
            )
        }
    }
}

export default App;
