import React, {useState} from 'react';
import NotesPane from "./NotesPane";
import {Col, Row} from "react-bootstrap";
import CustomerPane from "./CustomerPane";
import CustomerSelect from "./CustomerSelect";
import Customer, {CustomerGetter} from "../models/Customer";
import LogPane from "./LogPane";
import QuestionPane from "./QuestionPane";

const Home: React.FC = () => {
    const [customer, setCustomer]: any = useState(null as Customer | null);
    ;
    let loadCustomer: CustomerGetter = new CustomerGetter(setCustomer);

    return (
        <Row>
            <Col xs={12}>
                <Row className="p-3">
                    <Col xs={3}>
                        <Row className="p-3">
                            <h4>Select Customer</h4>
                            <CustomerSelect loadCustomer={loadCustomer}/>
                        </Row>
                        <Row className="p-3">
                            <Col xs={12}>
                                <h4>Account Details</h4>
                                <CustomerPane customer={customer}/>
                            </Col>
                        </Row>
                    </Col>
                    <Col xs={9}>
                        <Row className="p-3">
                            <Col xs={12}>
                                <h4>Customer Assistant</h4>
                                <QuestionPane customer={customer} loadCustomer={loadCustomer}/>
                            </Col>
                        </Row>
                    </Col>
                </Row>
                <Row className="p-3">
                    <Col xs={6}>
                        <h4>Notes</h4>
                        <NotesPane customer={customer}/>
                    </Col>
                    <Col/>
                    <Col xs={5}>
                        <h4>Log</h4>
                        <LogPane customer={customer}/>
                    </Col>
                </Row>
            </Col>
        </Row>
    );
};

export default Home;
