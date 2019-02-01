import React, { Component } from 'react';
import './App.css';
import firebase from "./Firestore.js";
import LoaderScreen from './LoaderScreen.js'
import Welcome from './Welcome.js'
import Paper from '@material-ui/core/Paper';
import Typography from '@material-ui/core/Typography';
import Button from '@material-ui/core/Button';
import ChartContainer from './ChartContainer';
import Fab from '@material-ui/core/Fab'
import AddIcon from '@material-ui/icons/Add';

class App extends Component {

  /** TODO: make meta-list "Devices:[Days:[x]]" in Firebase so as to not query too much data!
   */

  constructor() {
    super();
    this.state = {
      isLoading: true,
      width: 0,
      height: 0,
      amountOfDisplayedGraphs: [0],
      availableDevices: [],
    };

    this.updateWindowDimensions = this.updateWindowDimensions.bind(this);
    this.updateUser = this.updateUser.bind(this);
    this.toggleIsLoading = this.toggleIsLoading.bind(this);
    this.setLoading = this.setLoading.bind(this);
    this.logout = this.logout.bind(this);
    this.cleanUpLocalData = this.cleanUpLocalData.bind(this);
    this.fillDataForChart = this.fillDataForChart.bind(this);
    this.getListOfDevicesFromRTDB = this.getListOfDevicesFromRTDB.bind(this);
    this.addGraph = this.addGraph.bind(this);
    this.removeGraph = this.removeGraph.bind(this);
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
        //self.getDataFromRealtimeDBContinuously();
        self.getListOfDevicesFromRTDB();
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

  getListOfDevicesFromRTDB() {
    //get referene by getting the user's email (dots have been replaced by commas)
    let dbKey = this.state.user.email.replace(/\./g, ",");
    const dataRef = this.rtdb.ref(dbKey + "/devices");
    this.setLoading(true);
    let listOfDevicesExists = false;


    dataRef.once('value').then((snap) => {
      console.log("devices exist: " + snap.val());

      if (snap.exists()) {
        let _tempArray;
        dataRef.on('child_added', (devices) => {
          _tempArray = this.state.availableDevices;
          _tempArray.push(devices.val());
          this.setState({ availableDevices: _tempArray, isLoading: false });
        })
      } else {
        this.setState({ isLoading: false });
      }
    })
  }

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
              dataForChart.time = (snap_dataObject.val().date.hours).toString() + ":" + snap_dataObject.val().date.minutes.toString() + ":" + snap_dataObject.val().date.seconds.toString();

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
          var date = new Date(snap_dataObject.val().date.time);
          dataForChart.time = date.toLocaleTimeString();
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

  addGraph() {
    let _tempArray = this.state.amountOfDisplayedGraphs;
    _tempArray.push(1);
    this.setState({
      amountOfDisplayedGraphs: _tempArray,
    })
  }

  removeGraph(index) {
    console.log("remove: " + index)
    let _tempArray = this.state.amountOfDisplayedGraphs;
    _tempArray[index] = null;
    this.setState({
      amountOfDisplayedGraphs: _tempArray,
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
          //lastReceivedData = lastReceivedData.days[0];
          //lastReceivedData = lastReceivedData.data[0];
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

            {/* This is a bit tricky. Charts are associated with an arrayindex. If the index exists, the chart is drawn. When deleting a chart, the corresponding arrayIndex is nulled. This is done with an array bc we can't map a number to react-components */}
            {this.state.amountOfDisplayedGraphs.map((x, i) => {  
              if (x != null) { //x is 0 or 1, implying wheter the corresponding graph should be drawn (1=draw, null=ignore). i is the index of the corresponding graph, needed to delete the right graph
                return (
                  <Paper className="main_Welcome_graph">
                    <ChartContainer index={i} removeMyself={() => this.removeGraph(i)} devices={this.state.availableDevices} rtdb={this.rtdb} email={this.state.user.email} width={this.state.width} height={this.state.height} />
                  </Paper>
                )
              }
            })}

            {/* only display the "add Graph" button if list of devices exists */}
            {(typeof this.state.availableDevices != "undefined" && this.state.availableDevices.length > 0) ? 
            <Fab color="primary" aria-label="Add" className={"main_Welcome_add"}>
              <AddIcon onClick={() => this.addGraph()} />
            </Fab> : null }

          </div>
        );
      }

    }
  }
}

export default App;

