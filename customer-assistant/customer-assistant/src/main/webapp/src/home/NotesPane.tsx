import React, {FormEvent, MutableRefObject, useEffect, useRef, useState} from 'react';
import {Row} from 'react-bootstrap';
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import Customer from "../models/Customer";


const NotesPane: React.FC<{
    customer: Customer
}> = ({customer}) => {

    const [fact, setFact]: any = useState('');
    const factsBox: MutableRefObject<any> = useRef();

    function saveFacts(event: FormEvent) {
        event.preventDefault()
        const requestOptions = {
            method: 'POST',
            headers: {'Content-Type': 'application/text'},
            body: fact
        };

        fetch('/api/customer/setNotes/' + customer.uid, requestOptions)
    }

    useEffect(() => {
        if (customer != null) {
            setFact(customer.notes.notes.join("\n"));
            if (factsBox.current != undefined) {
                factsBox.current.value = fact;
            }
        }
    });

    function updateFact(newValue: String) {
        customer.notes.notes = newValue.split("\n");
    }

    return (
        <Form onSubmit={(event) => saveFacts(event)}>
            <Row>
                <Form.Control as="textarea"
                              ref={factsBox}
                              rows={10}
                              onChange={(e: any) => updateFact(e.currentTarget.value)}
                              disabled={false}/>

                <Button variant="primary" type={"submit"}>Save</Button>
            </Row>
        </Form>
    )
};

export default NotesPane;
