import React, {useEffect, useState} from 'react';
import {Row} from 'react-bootstrap';
import Form from 'react-bootstrap/Form';
import {CustomerGetter} from "../models/Customer";

const CustomerSelect: React.FC<{
    loadCustomer: CustomerGetter
}> = ({loadCustomer}) => {

    const [fc, setNames]: any = useState<Map<string, string>>();
    const [selectedName, setSelectedCustomer]: any = useState<string>();

    let names: Map<string, string> = fc;

    useEffect(() => {

        const requestOptions = {
            method: 'GET',
            headers: {'Content-Type': 'application/json'}
        };

        fetch('/api/customer/names', requestOptions)
            .then(response => {
                return response.json();
            })
            .then(json => {
                let nameIds = new Map(Object.entries(json)) as Map<string, string>;

                setNames(nameIds);
                setSelectedCustomer(nameIds.keys().next().value);
                loadCustomer.getById(nameIds.keys().next().value);
            })
    }, [])

    function getCustomer(id: string) {
        loadCustomer.getById(id)
            .then(() => {
                setSelectedCustomer(id);
            })
    }

    return (
        <Row>
            <Form.Select
                value={(selectedName != undefined) ? selectedName : ""}
                onChange={(e: any) => getCustomer(e.currentTarget.value)}>
                {
                    names !== undefined &&
                    Array.from(names.keys()).map(id => {
                        return <option value={id}>{names.get(id)}</option>
                    })
                }
            </Form.Select>
        </Row>
    )
};

export default CustomerSelect;
