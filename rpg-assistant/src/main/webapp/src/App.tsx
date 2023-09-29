import React from 'react';

import Container from 'react-bootstrap/Container';
import {Tab, Tabs} from "react-bootstrap";
import Home from "./home/home";
import Generate from "./generate/Generate";
import Rules from "./rules/Rules";


const App: React.FC = () => {
    return (
        <Container className="p-3">
            <Container>
                <h2 className="header">Forgetful RPG Assistant</h2>
            </Container>
            <hr/>
            <div className="mh-100">
                <Tabs className="mb-3" justify>
                    <Tab eventKey="home" title="Home">
                        <Home/>
                    </Tab>
                    <Tab eventKey="generate" title="Generate">
                        <Generate/>
                    </Tab>
                    <Tab eventKey="rules" title="Rules">
                        <Rules/>
                    </Tab>
                </Tabs>
            </div>
        </Container>
    );
};

export default App;
