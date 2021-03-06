import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve, RouterStateSnapshot} from '@angular/router';
import {ClientCsarsService} from '../../services/csar.service';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/observable/of';
import 'rxjs/add/operator/skipWhile';
import 'rxjs/add/operator/take';
import {Csar} from '../../model/csar';
import {isNullOrUndefined} from 'util';

@Injectable()
export class CsarResolver implements Resolve<Observable<Csar>> {

    constructor(private csarProvider: ClientCsarsService) {
    }

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<Csar> {
        let count = 1;
        if (this.csarProvider.getCount() === 0) {
            count = 2;
        }
        let res = this.csarProvider.csars.take(count).map(array => {
                return array.find(item => item.name === route.params['csarId']);
            }
        );
        if (count === 2) {
            res = res.skipWhile(data => !isNullOrUndefined(data));
        }
        return res;
    }

}
