import React, {useState} from 'react';
import {Col, Row} from "react-bootstrap";
import Form from "react-bootstrap/Form";
import Button from "react-bootstrap/Button";
import { Characters } from '../models/Characters';


const Generate: React.FC<{
    characters: Characters
}> = ({
    characters
}) => {

        const [request, setRequest]: any = useState("");
        const [generated, setGenerated]: any = useState("");
        const [loading, setLoading]: any = useState(false);

        function generate(event: HTMLFormElement) {
            event.preventDefault()
            setLoading(true)
            const requestOptions = {
                method: 'POST',
                headers: {'Content-Type': 'application/text'},
                body: request
            };

            fetch('/api/generate/request', requestOptions)
                .then(response => {
                    setLoading(false)
                    return response.text();
                })
                .then(text => {
                    setGenerated(text);
                })
        }

        return (
            <Row>
                <Col xs={12}>
                    <Row className="p-3">
                        <Form onSubmit={(event: any) => generate(event)}>
                            <Row>
                                <h4>Instructions</h4>
                                <Form.Control as="input"
                                              onChange={(e: any) => setRequest(e.currentTarget.value)}
                                              value={request}/>
                            </Row>
                            <Row>
                                <Button variant="primary" type={"submit"}>
                                    <span className="sr-only">Generate Content  </span>
                                    <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true"
                                          hidden={!loading}></span>
                                </Button>
                            </Row>
                        </Form>
                    </Row>
                    <Row className="p-3">
                        <Form>
                            <Row>
                                <h4>Generated Content</h4>
                                <Form.Control as="textarea"
                                              contentEditable={false}
                                              disabled={true}
                                              rows={20}
                                              value={generated == null ? "" : generated}/>
                            </Row>
                        </Form>
                    </Row>
                </Col>
            </Row>
        )
            ;
    }
;

export default Generate;
