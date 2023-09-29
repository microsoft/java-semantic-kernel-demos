import React from 'react';
import {Row} from 'react-bootstrap';
import Form from 'react-bootstrap/Form';
import {Log} from "../models/Log";

const LogPane: React.FC<{ gameLog: Log }> = ({gameLog}) => {

    return (
        <Form>
            <Row>
                <Form.Control as="textarea"
                              contentEditable={false}
                              disabled={true}
                              rows={30}
                              value={gameLog.log.join("\n")}/>
            </Row>
        </Form>
    )
};

export default LogPane;
