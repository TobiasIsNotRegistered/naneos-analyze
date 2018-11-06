import React, { Component } from 'react';
import logo from './logo.svg';
import './App.css';
import firebase from "./Firestore.js";
import CardView from './CardView.js';

class App extends Component {

  constructor() {
    super();
    this.state = {
      data: [],
      isLoading: true
    };
  }

  componentDidMount() {
    this.getFirestoreConnection();
    this.getDataFromFirestoreOnce();
    this.getDataFromFirestoreContinously();
  }

  getFirestoreConnection() {
    this.db = firebase.firestore();
    const settings = { timestampsInSnapshots: true };
    this.db.settings(settings);
  }

  getDataFromFirestoreOnce() {
    this.setState({ isLoading: true });

    var colRef = this.db.collection("/Customers/Naneos/TestProjektTobi");
    let temp = this.state.data;

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
        data: temp,
        isLoading: false
      });
    }
    )
  }

  getDataFromFirestoreContinously() {
    var colRef = this.db.collection("/Customers/Naneos/TestProjektTobi");
    let temp = this.state.data;

    //listen for changes
    colRef.onSnapshot(snapshot => {
      snapshot.docChanges().forEach(function (change) {
        if (change.type === "added") {
          temp.push(change.doc.data());
        }
        if (change.type === "modified") {

        }
        if (change.type === "removed") {
          temp.splice(temp.findIndex(data => data.date === change.doc.data().date), 1);
        }
      });
      //only change state via setState
      //temp.sort((e1, e2) => {return (e1.date.toDate() - e2.date.toDate())})
      this.setState({ data: temp });
    })
  }

  deleteAllElementsInFirestore() {
    const colRef = this.db.collection("/Customers/Naneos/TestProjektTobi");

    /* this.setState({isLoading: true}); */

    const numberOfEntries = this.state.data.length;
    let numberOfDeletions = 0;

    colRef.get().then((snapshot) => {
      snapshot.forEach((doc) => {
        doc.ref.delete()         
          .catch(e => console.log(e.message));
      })
    })
  }

  render() {
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
            Entries in DB: {this.state.data.length}
          </p>

          <div className="mainContent">
            {this.state.data
              .map(e => {
                id++;
                return <CardView dataObject={e} key={id}/>
              })}
          </div>
        </div>
      );
    }
  }
}

export default App;
