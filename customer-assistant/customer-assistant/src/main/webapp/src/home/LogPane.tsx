import React from 'react';
import Customer from "../models/Customer";


const LogPane: React.FC<{
    customer: Customer
}> = ({customer}) => {

    return (
        <ul>
            {
                customer?.log?.log
                    .map((key, value) => {
                        return <li
                            key={key.timestamp}>{new Date(Date.parse(key.timestamp)).toLocaleDateString()}: {key.event}</li>
                    })
            }
        </ul>
    )
};

export default LogPane;
