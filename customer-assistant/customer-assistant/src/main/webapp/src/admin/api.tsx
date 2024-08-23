class Api {
    static fetchApi(
        input: string | URL | globalThis.Request,
        init?: RequestInit): Promise<Response> {
        return fetch(input, init);
    }
}

export default Api;