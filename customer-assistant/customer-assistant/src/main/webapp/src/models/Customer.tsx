import Api from "../admin/api";

interface Notes {
    notes: string[];
}

interface LogEvent {
    timestamp: string;
    event: string;
}

interface Log {
    log: LogEvent[]
}

enum AccountType {
    STANDARD = "STANDARD",
    PREMIUM = "PREMIUM",
    ENTERPRISE = "ENTERPRISE"
}

enum AccountStatus {
    ACTIVE = "ACTIVE",
    SUSPENDED = "SUSPENDED",
    CLOSED = "CLOSED"
}

interface Customer {
    uid: string;
    name: string;
    notes: Notes;
    log: Log;
    accountType: AccountType;
    accountStatus: AccountStatus;
}

class CustomerGetter {
    constructor(setSelectedCustomer: any) {
        this.setCustomer = setSelectedCustomer;
    }

    private setCustomer: any;

    public getById(uid: string): Promise<Customer> {
        const requestOptions = {
            method: 'GET',
            headers: {'Content-Type': 'application/json'}
        };

        return Api.fetchApi('/api/customer/info/' + uid, requestOptions)
            .then(response => {
                return response.json();
            })
            .then(json => {
                this.setCustomer(json as Customer);
                return json as Customer;
            })
    }
}

export default Customer;
export {CustomerGetter};