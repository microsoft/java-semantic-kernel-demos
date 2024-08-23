import React, {FormEvent, useState} from 'react';
import {Col, Row, Spinner} from 'react-bootstrap';
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import Customer, {CustomerGetter} from "../models/Customer";
import Api from "../admin/api";

const QuestionPane: React.FC<{
    customer: Customer,
    loadCustomer: CustomerGetter
}> = ({customer, loadCustomer}) => {
    const [question, setQuestion]: any = useState('');
    const [continuedQuestion, setContinuedQuestion]: any = useState('');
    const [answer, setAnswer]: any = useState('');
    const [documents, setDocuments]: any = useState('');
    const [loading, setLoading]: any = useState(false);
    const [chatId, setChatId]: any = useState();

    function appendToAnswer(data: string) {
        setAnswer((current: string) => {
            if (current.length > 0) {
                return current + '\n\n' + data
            } else {
                return data;
            }
        });
    }

    function sendQuestion(question: string, chatId: string): Promise<Response> {
        const requestOptions = {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'chatId': chatId
            },
            body: question
        };

        return Api.fetchApi('/api/customer/message/' + customer.uid, requestOptions);
    }

    function askQuestionContinued(event: FormEvent) {
        event.preventDefault()
        setLoading(true)
        appendToAnswer("            YOU: " + continuedQuestion);
        sendQuestion(continuedQuestion, chatId)
            .then(response => {
                return response.text();
            })
            .then(json => {
                let data = json as string;
                appendToAnswer(data)
                setLoading(false)
                loadCustomer.getById(customer.uid);

                //setDocuments(event.data)
            });
    }

    function generateId() {
        let result = '';
        const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
        for (let i = 0; i < 30; i++) {
            result += characters.charAt(Math.floor(Math.random() * characters.length));
        }
        return result;
    }

    function askQuestion(event: FormEvent) {
        event.preventDefault()
        setLoading(true)
        setAnswer("");
        setDocuments("");

        // Not intended to be a secure id, just an id for this chat session
        const id = generateId();
        setChatId(id);

        sendQuestion(question, id)
            .then(response => {
                return response.text();
            })
            .then(json => {
                let data = json as string;
                appendToAnswer(data)
                setLoading(false)
                loadCustomer.getById(customer.uid);

                //setDocuments(event.data)
            });
    }

    return (
        <Row>
            <Form onSubmit={(event) => askQuestion(event)}>
                <Row className="mb-5">
                    <Col xs={11}>
                        <Row>
                            <Form.Control as="input"
                                          value={question}
                                          onChange={(e: any) => setQuestion(e.currentTarget.value)}/>
                            <Button variant="primary" type={"submit"}>Ask New Question/Give New Instruction</Button>
                        </Row>
                    </Col>
                    <Col xs={1}>
                        <Row>
                            {
                                loading != undefined && loading &&
                                <Spinner animation="border"/>
                            }
                        </Row>
                    </Col>
                </Row>
            </Form>
            <Form onSubmit={(event) => askQuestionContinued(event)}>
                <Row>
                    <Col>
                        <Row className="end-0">
                            <Form.Control as="textarea"
                                          contentEditable={false}
                                          disabled={true}
                                          rows={5}
                                          value={answer}/>
                        </Row>
                        <Row className="pt-1">
                            <Col xs={3}>
                                <p>Continue conversation:</p>
                            </Col>
                            <Col xs={9}>
                                <Form.Control as="input"
                                              value={continuedQuestion}
                                              onChange={(e: any) => setContinuedQuestion(e.currentTarget.value)}/>
                            </Col>
                        </Row>
                        {
                            documents != undefined && documents.length > 0 &&
                            <Row>
                                <h5>Related documents</h5>
                                <div dangerouslySetInnerHTML={{__html: documents}}/>
                            </Row>
                        }
                    </Col>
                </Row>
            </Form>
        </Row>
    )
};

export default QuestionPane;
