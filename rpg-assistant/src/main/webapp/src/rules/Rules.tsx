import React, {useEffect, useState} from 'react';
import {Col, Row} from "react-bootstrap";
import Form from "react-bootstrap/Form";
import {WorldRules} from "../models/WorldRules";
import Button from "react-bootstrap/Button";


const Rules: React.FC<{}> = ({}) => {

        const [worldRulesStr, setWorldRules]: any = useState(null as String | null);
        let worldRules: WorldRules = new WorldRules(setWorldRules);

        const [lastRequest, setLastRequest]: any = useState(null as number | null);

        useEffect(() => {
            reloadData()
        }, [])

        function updateWorldRule(newValue: String) {
            setWorldRules(newValue);

            if (lastRequest) {
                window.clearTimeout(lastRequest);
            }

            var l = setTimeout(() => {
                worldRules.push(newValue);
            }, 2000);
            setLastRequest(l);
        }

        function reloadData() {
            worldRules.get();
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
                                <h4>World Rules</h4>
                                <Form.Control as="textarea"
                                              contentEditable={true}
                                              disabled={false}
                                              rows={20}
                                              onChange={(e: any) => updateWorldRule(e.currentTarget.value)}
                                              value={worldRulesStr == null ? "" : worldRulesStr}/>
                            </Row>
                        </Form>
                    </Row>
                </Col>
            </Row>
        );
    }
;

export default Rules;
