import React, { Component } from 'react';
import './App.css';
import firebase from "./Firestore.js";
import LoaderScreen from './LoaderScreen.js'
import Welcome from './Welcome.js'

import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, Brush, ReferenceLine } from 'recharts';
import ChartContainer from './ChartContainer';

class App extends Component {

  constructor() {
    super();
    this.state = {
      isLoading: false,
      width: 0,
      height: 0,      

      availableDevices : []
    };

    this.dataStore = [];
    this.updateWindowDimensions = this.updateWindowDimensions.bind(this);
    this.updateUser = this.updateUser.bind(this);
    this.toggleIsLoading = this.toggleIsLoading.bind(this);
    this.setLoading = this.setLoading.bind(this);
    this.logout = this.logout.bind(this);
    this.cleanUpLocalData = this.cleanUpLocalData.bind(this);
  }

  /** ***************** AUXILIARY ***************** **/
  toggleIsLoading(){
    this.state.isLoading ? this.setState({isLoading:false}) : this.setState({isLoading: true});
  }

  setLoading(boolean){
    this.setState({
      loading: boolean
    })
  }

  updateUser(user){
    console.log("updated User!");
    this.setState({
      user: user
    })
  }

  logout(){
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

  cleanUpLocalData(){
    this.setState({
      availableDevices: []
    })
    this.dataStore = [];
  }

  
/** ***************** Lifecycle ***************** **/
  componentDidMount() {
    this.updateWindowDimensions();
    window.addEventListener('resize', this.updateWindowDimensions);
    var self = this;
    this.getRealtimeDBConnection();

    firebase.auth().onAuthStateChanged(function(user) {
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

  getDataFromRealtimeDBContinuously() {
    let dbKey = this.state.user.email.replace(/\./g, ",");
    const dataRef = this.rtdb.ref(dbKey);    
    
    dataRef.on('child_added', (e) => {
      console.log("Key: " + e.key);
      let tempArray = this.state.availableDevices;
      tempArray.push(e.key);
      this.setState({availableDevices: tempArray});
      tempArray = null;

      e.forEach(e => {
        console.log("-Date: " + e.key);

        e.forEach(i => {
            //console.log("--DataObj: " + i.key);
            //console.log("--LDSA: " + i.val().ldsa);   
          
        })
      })
      /*

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
          numberC: e.val().numberC,
          temp: e.val().temp > 0 ? e.val().temp : null,
        };

        if (this.state.currentMaxValueLDSA < chartObject.ldsa) {
          this.setState({ currentMaxValueLDSA: chartObject.ldsa,
           });
        }

        this.dataStore.push(chartObject);

        */
    })
  }

  /** ***************** RENDER ***************** **/
  render() {    

    if (this.state.isLoading || this.state.availableDevices.length < 1) {
      return <LoaderScreen />
    } else {

      let current_chart_data = this.dataStore.sort((a,b) => b.date - a.date); // .slice(0, this.state.amountOfDataInChart);

      if(!this.state.user){
        return (
          <div className="App">
            <Welcome setLoading={this.setLoading}/>  
          </div>
        );
      }else{
        return(
          <div className="main">
            <p>Welcome, you are now logged in as: {this.state.user.email}</p>
            <button onClick={() => this.logout()}>LogOut!</button>
            <p>Available Devices: </p>
            {this.state.availableDevices.map(string => {
              return(
                <div>{string}</div>
              )
            })}
            <ChartContainer data={current_chart_data} dataStore = {this.dataStore}/>
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