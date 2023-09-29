import React from 'react';
import {Col, Row} from 'react-bootstrap';
import Form from 'react-bootstrap/Form';
import Customer from "../models/Customer";


const CustomerPane: React.FC<{ customer: Customer | null }> = ({customer}) => {

    if (customer === null) {
        return <div></div>
    }
    return (
        <Form>
            <Row>
                <Col xs={6}>
                    <dl>
                        <dt>Name</dt>
                        <dl>{customer.name}</dl>
                        <dt>Account Type</dt>
                        <dl>{customer.accountType}</dl>
                        <dt>Status</dt>
                        <dl>{customer.accountStatus}</dl>
                    </dl>
                </Col>
            </Row>
        </Form>
    )
};

export default CustomerPane;
