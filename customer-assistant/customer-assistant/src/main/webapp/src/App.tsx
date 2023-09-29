import React from 'react';

import Container from 'react-bootstrap/Container';
import {Tab, Tabs} from "react-bootstrap";
import Admin from "./admin/admin";
import Home from "./home/Home";


const App: React.FC = () => {
    return (
        <Container className="p-3">
            <Container>
                <h2 className="header">Customer Manager</h2>
            </Container>
            <hr/>
            <div className="mh-100">
                <Tabs className="mb-3" justify>
                    <Tab eventKey="home" title="Home">
                        <Home/>
                    </Tab>
                    <Tab eventKey="admin" title="Admin">
                        <Admin/>
                    </Tab>
                </Tabs>
            </div>
        </Container>
    );
};

export default App;
