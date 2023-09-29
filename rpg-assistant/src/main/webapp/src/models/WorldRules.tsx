class WorldRules {
    constructor(setWorldRules: any) {
        this.setWorldRules = setWorldRules;
    }

    private setWorldRules: any;

    public get(): Promise<String> {
        const requestOptions = {
            method: 'GET',
            headers: {'Content-Type': 'application/text'}
        };

        return fetch('/rules/worldRules', requestOptions)
            .then(response => {
                return response.text();
            })
            .then(json => {
                let data = json as String;
                this.setWorldRules(data);
                return data;
            })
    }

    push(worldRulesStr: any) {
        const requestOptions = {
            method: 'POST',
            headers: {'Content-Type': 'application/text'},
            body: worldRulesStr
        };

        fetch('/rules/worldRules', requestOptions)

    }
}

export {WorldRules};