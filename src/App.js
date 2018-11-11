import React, { Component } from 'react';
import logo from './logo.svg';
import './App.css';
import firebase from "./Firestore.js";
import CardView from './CardView.js';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend } from 'recharts';

class App extends Component {

  constructor() {
    super();
    this.state = {
      raw_data: [],
      ldsa_data: [],
      isLoading: false,
      width: 0,
      height: 0
    };

    this.updateWindowDimensions = this.updateWindowDimensions.bind(this);
  }

  componentDidMount() {
    this.getFirestoreConnection();
    //this.getDataFromFirestoreOnce();
    this.getDataFromFirestoreContinously();

    this.updateWindowDimensions();
    window.addEventListener('resize', this.updateWindowDimensions);
  }

  componentWillUnmount() {
    window.removeEventListener('resize', this.updateWindowDimensions);
  }

  getFirestoreConnection() {
    this.db = firebase.firestore();
    const settings = { timestampsInSnapshots: true };
    this.db.settings(settings);
  }

  getDataFromFirestoreOnce() {
    this.setState({ isLoading: true });

    var colRef = this.db.collection("/Customers/Naneos/TestProjektTobi");
    let temp = this.state.raw_data;

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

  getDataFromFirestoreContinously() {
    const colRef = this.db.collection("/Customers/Naneos/TestProjektTobi");
    let raw_temp = this.state.raw_data;
    let ldsa_temp = this.state.ldsa_data;

    //listen for changes
    colRef.onSnapshot(snapshot => {
      snapshot.docChanges().forEach(function (change) {
        if (change.type === "added") {
          raw_temp.push(change.doc.data());

          const lineChartObject = { name: change.doc.data().date.toDate().toLocaleString(), ldsa: change.doc.data().ldsa, date: change.doc.data().date };
          ldsa_temp.push(lineChartObject);
        }
        if (change.type === "modified") {

        }
        if (change.type === "removed") {
          raw_temp.splice(raw_temp.findIndex(data => data.date === change.doc.data().date), 1);
        }
      });
      //only change state via setState
      raw_temp.sort((e1, e2) => { return (e2.date.toDate() - e1.date.toDate()) })
      ldsa_temp.sort((e1, e2) => { return (e2.date.toDate() - e1.date.toDate()) })
      this.setState({
        data: raw_temp,
        ldsa_data: ldsa_temp
      });
    })
  }

  deleteAllElementsInFirestore() {
    const colRef = this.db.collection("/Customers/Naneos/TestProjektTobi");

    /* this.setState({isLoading: true}); */

    const numberOfEntries = this.state.raw_data.length;
    let numberOfDeletions = 0;

    colRef.get().then((snapshot) => {
      snapshot.forEach((doc) => {
        doc.ref.delete()
          .catch(e => console.log(e.message));
      })
    })
  }

  updateWindowDimensions() {
    this.setState({ width: window.innerWidth, height: window.innerHeight });
  }

  render() {

    let data = this.state.ldsa_data.slice(0, 200);;

    if (this.state.isLoading) {
      return <p>Loading...</p>
    } else {
      let id = 0;
      return (
        <div className="App">
          <header className="App-header">
            <h1 className="App-title">Naneos Analyze</h1>
          </header>

          <button onClick={() => this.deleteAllElementsInFirestore()}>Delete!</button>

          <p className="App-intro">
            Entries in DB: {this.state.raw_data.length}
          </p>


          <LineChart width={0.9 * this.state.width} height={300} data={data} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
            <XAxis dataKey="name" />
            <YAxis />
            <CartesianGrid strokeDasharray="3 3" />
            <Tooltip />
            <Legend />
            <Line type="monotone" dataKey="ldsa" stroke="#8884d8" activeDot={{ r: 8 }} />
          </LineChart>

          <div className="mainContent">
            {this.state.raw_data
              .map(e => {
                id++;
                return <CardView dataObject={e} key={id} />
              })}
          </div>
        </div>
      );
    }
  }
}

export default App;
