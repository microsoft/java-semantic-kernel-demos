import Api from "../admin/api";

class CustomerRules {
    constructor(setCustomerRules: any) {
        this.setCustomerRules = setCustomerRules;
    }

    private setCustomerRules: any;

    public get(): Promise<String> {
        const requestOptions = {
            method: 'GET',
            headers: {'Content-Type': 'application/text'}
        };

        return Api.fetchApi('/api/admin/rules', requestOptions)
            .then(response => {
                return response.text();
            })
            .then(json => {
                let data = json as String;
                this.setCustomerRules(data);
                return data;
            })
    }

    push(worldCustomerStr: any) {
        const requestOptions = {
            method: 'POST',
            headers: {'Content-Type': 'application/text'},
            body: worldCustomerStr
        };

        Api.fetchApi('/api/admin/rules', requestOptions)

    }
}

export {CustomerRules};