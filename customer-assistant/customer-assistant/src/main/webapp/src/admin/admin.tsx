import React, {useEffect, useState} from 'react';
import {Col, Row} from "react-bootstrap";
import Form from "react-bootstrap/Form";
import {CustomerRules} from "../models/CustomerRules";
import Button from "react-bootstrap/Button";


const Admin: React.FC<{}> = ({}) => {

        const [customerRulesStr, setCustomerRules]: any = useState(null as String | null);
        let customerRules: CustomerRules = new CustomerRules(setCustomerRules);

        const [lastRequest, setLastRequest]: any = useState(null as number | null);

        useEffect(() => {
            reloadData()
        }, [])

        function updateCustomerRule(newValue: String) {
            setCustomerRules(newValue);

            if (lastRequest) {
                window.clearTimeout(lastRequest);
            }

            var l = setTimeout(() => {
                customerRules.push(newValue);
            }, 2000);
            setLastRequest(l);
        }

        function reloadData() {
            customerRules.get();
        }

        return (
            <Row>
                <Col xs={3}>
                    <Button onClick={reloadData}>Refresh</Button>
                </Col>
                <Col xs={12}>
                    <Row className="p-3">
                        <Form>
                            <Row>
                                <h4>Customer Processing Rules</h4>
                                <Form.Control as="textarea"
                                              contentEditable={true}
                                              disabled={false}
                                              rows={10}
                                              onChange={(e: any) => updateCustomerRule(e.currentTarget.value)}
                                              value={customerRulesStr == null ? "" : customerRulesStr}/>
                            </Row>
                        </Form>
                    </Row>
                </Col>
            </Row>
        );
    }
;

export default Admin;
