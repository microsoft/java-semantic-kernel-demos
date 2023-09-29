import React, {useEffect, useState} from 'react';
import {Row} from 'react-bootstrap';
import Form from 'react-bootstrap/Form';
import {CharacterGetter} from "../models/Character";

const CharSelect: React.FC<{
    loadCharacter: CharacterGetter
}> = ({loadCharacter}) => {

    const [fc, setNames]: any = useState<Map<string, string>>();
    const [selectedName, setSelectedCharacter]: any = useState<string>();

    let names: Map<string, string> = fc;

    useEffect(() => {

        const requestOptions = {
            method: 'GET',
            headers: {'Content-Type': 'application/json'}
        };

        fetch('/players/names', requestOptions)
            .then(response => {
                return response.json();
            })
            .then(json => {
                let nameIds = new Map(Object.entries(json)) as Map<string, string>;

                setNames(nameIds);
                setSelectedCharacter(nameIds.keys().next().value);
                loadCharacter.getById(nameIds.keys().next().value);
            })
    }, [])

    function getCharacter(id: string) {
        loadCharacter.getById(id)
            .then(() => {
                setSelectedCharacter(id);
            })
    }


    return (
        <Row>
            <Form.Select
                value={(selectedName != undefined) ? selectedName : ""}
                onChange={(e: any) => getCharacter(e.currentTarget.value)}>
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

export default CharSelect;
