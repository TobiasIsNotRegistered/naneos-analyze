import React, { Component } from 'react';
import './App.css';
import firebase from "./Firestore.js";
import LoaderScreen from './LoaderScreen.js'
import Welcome from './Welcome.js'
import Paper from '@material-ui/core/Paper';
import Typography from '@material-ui/core/Typography';
import Button from '@material-ui/core/Button';
import ExpansionPanel from '@material-ui/core/ExpansionPanel';
import ExpansionPanelSummary from '@material-ui/core/ExpansionPanelSummary';
import ExpansionPanelDetails from '@material-ui/core/ExpansionPanelDetails';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import ChartContainer from './ChartContainer';
import { Divider, ListItem } from '@material-ui/core';
import { List } from '@material-ui/core';

class App extends Component {

  /** TODO: make meta-list "Devices:[Days:[x]]" in Firebase so as to not query too much data!
   */

  constructor() {
    super();
    this.state = {
      isLoading: true,
      width: 0,
      height: 0,

      availableDevices: [],
      chartData: [],
      currentlyViewedDayOfDevice1: "",
      chartisLoading: false,
    };

    this.updateWindowDimensions = this.updateWindowDimensions.bind(this);
    this.updateUser = this.updateUser.bind(this);
    this.toggleIsLoading = this.toggleIsLoading.bind(this);
    this.setLoading = this.setLoading.bind(this);
    this.logout = this.logout.bind(this);
    this.cleanUpLocalData = this.cleanUpLocalData.bind(this);
    this.fillDataForChart = this.fillDataForChart.bind(this);
  }

  /** ***************** AUXILIARY ***************** **/
  toggleIsLoading() {
    this.state.isLoading ? this.setState({ isLoading: false }) : this.setState({ isLoading: true });
  }

  setLoading(boolean) {
    this.setState({
      isLoading: boolean
    })
  }

  updateUser(user) {
    console.log("updated User!");
    this.setState({
      user: user
    })
  }

  logout() {
    firebase.auth().signOut();
    this.cleanUpLocalData();
    console.log("user signed out!")
  }

  getRealtimeDBConnection() {
    this.rtdb = firebase.database();
  }

  updateWindowDimensions() {
    this.setState({ width: window.innerWidth, height: window.innerHeight });
  }

  cleanUpLocalData() {
    this.setState({
      availableDevices: [],
      chartData: []
    })
  }


  /** ***************** Lifecycle ***************** **/
  componentWillMount() {
    this.setLoading(true);
  }

  componentDidMount() {
    this.updateWindowDimensions();
    window.addEventListener('resize', this.updateWindowDimensions);
    var self = this;
    this.getRealtimeDBConnection();

    firebase.auth().onAuthStateChanged(function (user) {
      if (user) {
        // User is signed in.
        console.log("Success!");
        console.log(user.email)
        self.setLoading(false);
        self.updateUser(user);
        self.getDataFromRealtimeDBContinuously();
      } else {
        // No user is signed in.
        self.setLoading(false);
        self.updateUser(null);
        console.log("no user signed in!");
      }
    });
  }

  componentWillUnmount() {
    window.removeEventListener('resize', this.updateWindowDimensions);
    this.cleanUpLocalData();
  }


  /** ***************** DB & SYNC ***************** **/


  getDataFromRealtimeDBContinuously() {
    //get referene by getting the user's email (dots have been replaced by commas)
    let dbKey = this.state.user.email.replace(/\./g, ",");
    const dataRef = this.rtdb.ref(dbKey);
    this.setLoading(true);

    let tempArray;
    //TODO: revoke limit! (used for development) limitToFirst(1)
    //Get all devices...
    dataRef.on('child_added', (snap_dataPerDevice) => {
      this.setLoading(true);
      console.log("Found Data from Device: " + snap_dataPerDevice.key);

      let dataPerDevice = {
        serial: snap_dataPerDevice.key,
        days: []
      }

      snap_dataPerDevice.forEach(snap_dataPerDay => {
        console.log("found a day")
        let dataPerDay = {
          day: snap_dataPerDay.key,
          data: []
        }

        //registering listeners for all days and devices
        //TODO: this doesn't work as intended (and not very elegant) needs update!
        let dbKeyForListener = dbKey + "/" + snap_dataPerDevice.key + "/" + snap_dataPerDay.key;
        console.log("registered listeners on: " + dbKeyForListener);
        const dbRefForListener = this.rtdb.ref(dbKeyForListener);
        dbRefForListener.endAt().limitToLast(1).on('child_added', (snap_dataObject) => {
          console.log(snap_dataObject.val());

          if (this.state.availableDevices != null && this.state.availableDevices.length > 0) {
            console.log("Update of Chart should happen!")
            let _tempArray = this.state.availableDevices;
            let device = _tempArray.find(function (device) { return device.serial == snap_dataObject.val().serial });

            if (device) {
              let dataForChart = snap_dataObject.val();
              dataForChart.time = snap_dataObject.val().date.hours.toString() + ":" + snap_dataObject.val().date.minutes.toString() + ":" + snap_dataObject.val().date.seconds.toString();

              let _tempArray = this.state.chartData;

              //update chart if user has clicked on it
              if (this.state.chartData.length > 0 && this.state.chartData[0].serial == dataForChart.serial) {
                _tempArray.push(dataForChart);

                console.log("chartData updated!");
                this.setState({
                  chartData: _tempArray
                })
              }
            }
          }
        })

        snap_dataPerDay.forEach(snap_dataObject => {
          dataPerDay.time = snap_dataObject.val().date.time;
          let dataForChart = snap_dataObject.val();
          dataForChart.time = snap_dataObject.val().date.hours.toString() + ":" + snap_dataObject.val().date.minutes.toString() + ":" + snap_dataObject.val().date.seconds.toString();
          dataPerDay.data.push(dataForChart);
        })

        dataPerDevice.days.push(dataPerDay);
      });

      tempArray = this.state.availableDevices;
      tempArray.push(dataPerDevice);
      this.setState({ availableDevices: tempArray });
      this.setLoading(false);
    })

   
  }


  fillDataForChart(_data) {
    let maxAmountData = 1000;

    if (_data.length > maxAmountData) {
      _data = _data.slice(_data.length - maxAmountData, _data.length);
    }

    this.setState({
      chartisLoading: true,
      chartData: _data,
    })
  }

  /** ***************** RENDER ***************** **/
  render() {
    //user not logged in
    if (!this.state.user) {
      return (
        <div className="App">
          <Welcome setLoading={this.setLoading} />
        </div>
      );
      //user is logged in
    } else {
      if (this.state.isLoading) {
        return <LoaderScreen />
      } else {

        let lastReceivedData = "null";

        //TODO: FIND LATEST DATABOBJECT
        //ALSO, don't do it here but rather when the data arrives
        if (this.state.availableDevices.length > 1) {
          lastReceivedData = this.state.availableDevices[0];
          lastReceivedData = lastReceivedData.days[0];
          lastReceivedData = lastReceivedData.data[0];
        }

        return (
          <div className="main">

            <Paper className="main_Welcome_header">

              <div className="main_Welcome_header_lhs">
                <Typography variant="h5" component="h3">
                  Naneos - Analyze
              </Typography>
              </div>

              <div className="main_Welcome_header_rhs">
               
                
                <Button onClick={() => this.logout()} className="main--Welcome_btnLogout">
                  LogOut {this.state.user.email}
              </Button>
              </div>
            </Paper>

            <Paper className="main_Welcome graph">
              <ChartContainer data={this.state.chartData} width={this.state.width} height={this.state.height} />
            </Paper>

            <Paper className="main_Welcome graph">
              <ChartContainer data={this.state.chartData} width={this.state.width} height={this.state.height} />
            </Paper>


            <Paper className="main_Welcome">
              <div className="main_Welcome_containter">
                <Typography variant="h5" component="h3">
                  Available Devices: {this.state.availableDevices.length < 1 ? "loading..." : this.state.availableDevices.length}
                </Typography>

                {/** For each device, each day, each dataObject, each data */}
                <div className="main_Welcome_epContainer">
                  {this.state.availableDevices.map(device => {
                    return (
                      <ExpansionPanel className="main_Welcome_ep" >
                        <ExpansionPanelSummary expandIcon={<ExpandMoreIcon />}>
                          <Typography>{device.serial}</Typography>
                        </ExpansionPanelSummary>
                        <ExpansionPanelDetails className="main_Welcome_epDetails">
                          <List>
                            {device.days
                              .sort((day1, day2) => { return (day2.time - day1.time) })
                              .map(day => {
                                return (
                                  <ListItem>
                                    <div onClick={() => this.fillDataForChart(day.data)} className="main_Welcome_epDetails_btn">
                                      <Button>{day.day} - {day.data.length} Records</Button>
                                      <Divider />
                                    </div>
                                  </ListItem>
                                )
                              })}
                          </List>
                        </ExpansionPanelDetails>
                      </ExpansionPanel>
                    )
                  })}
                </div>

                <div className="main_Welcome_lastReceived">
                  <Paper>
                    Last Received Data
                  <br /> Serial: {lastReceivedData.serial}
                    <br /> LDSA: {lastReceivedData.ldsa}
                    <br /> Humidity: {lastReceivedData.humidity}
                    <br /> Temp: {lastReceivedData.temp}
                    <br /> Date: {lastReceivedData.dateAsFirestoreKey}
                  </Paper>
                </div>


              </div>
            </Paper>

            <Paper className="main_Welcome graph">
              <ChartContainer data={this.state.chartData} width={this.state.width} height={this.state.height} />
            </Paper>
          </div>
        );
      }

    }
  }
}

export default App;


/*
  getFirestoreConnection() {
    this.db = firebase.firestore();
    const settings = { timestampsInSnapshots: true };
    this.db.settings(settings);
  }

  getDataFromFirestoreOnce() {
    this.setState({ isLoading: true });

    var colRef = this.db.collection("/Customers/Naneos/TestProjektTobi");
    let temp = this.state.rawData;

    //retrieve data one by one, store in array 'temp'
    colRef.get().then((snapshot) => {
      snapshot.forEach((doc) => {
        temp.push(doc.data())
      })
    }

      //after retrieving, sort the temp-array
    ).then(() => {
      temp.sort((e1, e2) => { e1.date >= e2.date });
      //only change state via setState
      this.setState({
        raw_data: temp,
        isLoading: false
      });
    }
    )
  }

  getDataFromFirestoreContinuously() {
    const colRef = this.db.collection("/Customers/Naneos/TestProjektTobi");
    let _raw_temp = this.state.rawData;
    let _chart_data_temp = this.state.chartData;

    this.setState({ isLoading: true });

    //listen for changes
    colRef.orderBy('date').limit(100).onSnapshot(snapshot => {
      snapshot.docChanges().forEach(function (change) {
        if (change.type === "added") {
          _raw_temp.push(change.doc.data());

          const chartObject = {
            name: change.doc.data().date.toDate().toLocaleString(),
            ldsa: change.doc.data().ldsa,
            humidty: change.doc.data().humidty,
            temp: change.doc.data().temp ,
            date: change.doc.data().date
          };
          _chart_data_temp.push(chartObject);
        }
        if (change.type === "modified") {

        }
        if (change.type === "removed") {
          _raw_temp.splice(_raw_temp.findIndex(data => data.date === change.doc.data().date), 1);
        }
      });
      //only change state via setState
      _raw_temp.sort((e1, e2) => { return (e2.date.toDate() - e1.date.toDate()) })
      _chart_data_temp.sort((e1, e2) => { return (e2.date.toDate() - e1.date.toDate()) })
      this.setState({
        rawData: _raw_temp,
        chartData: _chart_data_temp,
        isLoading: false
      });
    })
  }



  deleteAllElementsInFirestore() {
    const colRef = this.db.collection("/Customers/Naneos/TestProjektTobi");

    /* this.setState({isLoading: true}); */
/*

const numberOfEntries = this.state.rawData.length;
let numberOfDeletions = 0;

colRef.get().then((snapshot) => {
  snapshot.forEach((doc) => {
    doc.ref.delete()
      .catch(e => console.log(e.message));
  })
})
}

*/

/*

getDataFromRealtimeDBOnce() {
    let dataRef = this.rtdb.ref('data');
    console.log(new Date(1542123554414).toLocaleString());
    this.setState({
      isLoading: true
    })

    dataRef = dataRef.orderByChild('date/time');
    dataRef.once('value').then((snapshot) => {
      snapshot.forEach((e) => {
        const date = new Date(e.val().date.time);
        const chartObject = {
          date: date,
          time: date.toLocaleTimeString(),
          dateLong: date.toLocaleString(),
          dateShort: date.toLocaleDateString(),
          diameter: e.val().diameter > 0 ? e.val().diameter : null,
          error: e.val().error,
          batteryVoltage: e.val().batteryVoltage > 0 ? e.val().batteryVoltage : null,
          ldsa: e.val().ldsa > 0 ? e.val().ldsa : null,
          humidity: e.val().humidt > 0 ? e.val().humidity : null,
          numberC: e.val().numberC > 0 ? e.val().numberC : null,
          temp: e.val().temp > 0 ? e.val().temp : null,
        };

        this.dataStore.push(chartObject);
      })

      this.dataStore.sort((a, b) => b.date - a.date);

      this.setState({
        isLoading: false
      })
    });
  }

  */