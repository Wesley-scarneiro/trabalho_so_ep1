/*
 * Representar o escalonador do sistema operacional.
	- Cont�m uma vari�vel para indicar o processo que est� usando a CPU;
	- Cont�m uma fila de processos prontos, ORDENADOS POR PRIORIDADE;
	- Cont�m uma fila de processos bloqueados, ORDENADOS POR ORDEM DE CHEGADA;
	- Cont�m uma vari�vel quantum para indicar o intervalo de tempo em que um processo poder� fazer uso da CPU.
	� defino na instancia��o da classe, a partir da leitura do arquivo quantum.txt;
	
	- A classe possui os seguintes m�todos:
		- Distribui��o de cr�ditos: Cada processo, na fila de prontos, ter� o seu cr�dito inicial igual a sua prioridade;
		- Ordenar a fila de prontos: Ordenar a fila de processos prontos por ordem de cr�dito, do maior para o menor;
		- Executar processo: 
			- concede a CPU para um processo; 
			- a cada comando/instru��o o processo perde 1 de cr�dito; 
			- quando o cr�dito do processo acabar ser� recolocado na fila de prontos (necess�rio reordenar a fila de prontos); 
			- Quando TODOS os processos estiverem com ZERO de cr�dito, os cr�ditos de cada processo ser�o redistribu�dos e a fila 
			de prontos reeordenada. Depois disso, os processos voltam a executar.
			- Se um processo for bloqueado durante a sua execu��o na CPU, por uma chamada de E/S:
				- Processo � marcado como bloqueado, seu contexto � salvo no BPC e enviado para a fila de bloqueados;
				- Atribui-se um tempo de espera para o processo aguardar o desbloqueio: para cada ada processo que terminar de executar na CPU, o
				tempo de espera de todos os bloqueados ser�o decrementados. Um processo � desbloqueado quando 2 outros processos j� terminaram seu quantum. 
				(O tempo de espera pode ser definido como 2 vezes o n�mero de quantum de um processo).
				- Quando o tempo de espera de um bloqueado acabar, o processo ter� seu contexto restaurado e ser� enviado para a fila de prontos 
				(necess�rios reeordenar a fila de prontos). O processo entra na fila de prontos e executa a pr�xima instru��o guardada no PC (salva no BCP);
				
	- Se a fila de prontos estiver vazia e existir somente bloqueados, o tempo de espera de todos os processos bloqueados ser�o decrementados. Assim, uma hora
	ou outra haver� um processo na fila de prontos para executar.
	- Quando um processo executar a instru��o SAIDA, deixar� de executar na CPU, sair� da fila de prontos e da tabela de processos (perdendo tamb�m o BPC). 
 */

package trabalho_so;

import java.io.FileNotFoundException;
import java.util.*;

public class Escalonador {
	
	private Processo processoEmExecucao = null;								// Processo escolhido para usar a CPU.
	private Queue<Processo> filaDeProntos = new PriorityQueue<>();			// Ordena��o por cr�ditos.
	private List<Processo> filaDeBloqueados = new LinkedList<>();			// Ordena��o natural (FIFO).
	private int quantum;
	private int tempoEspera = 2;	
	
	/*
	 	Um escalonador � instanciado ao receber o valor do quantum.
	 */
	public Escalonador(int quantum) {
		
		this.quantum = quantum;
	}
	
	/*
	 * M�todo para adicionar um processo na fila de prontos.
	 * A inser��o � por ordem de cr�ditos.
	 * Processos com maiores quantidade de cr�ditos ficam entre os primeiros.
	 * Processos com menores quantidade de cr�ditos ficam entre os �ltimos.
	 */
	public void adicionarFilaProntos(Processo p) {
		
		filaDeProntos.add(p);
		
	}
	
	/*
	 * M�todo para remover um processo na fila de prontos.
	 * Remove o primeiro da fila e retorna a sua refer�ncia.
	 * Retorna null se n�o houver mais processos na fila para retirar.
	 */
	public Processo removerFilaProntos() {
		
		return filaDeProntos.poll();
	}
	
	/*
	 * Adiciona um processo na fila de bloqueados.
	 * Segue a ordem de chegada: primeiro a entrar � o primeiro a sair (FIFO).
	 */
	public void adicionarFilaBloqueados(Processo p) {
		
		filaDeBloqueados.add(p);
	}
	
	/*
	 * Remove um processo da fila de bloqueados.
	 * Retorna a refer�ncia do processo retirado (sempre o primeiro da fila).
	 * Se n�o houver processos bloqueados, retorna null.
	 */
	public Processo removerFilaBloqueados() {
		
		if (filaDeBloqueados.isEmpty()) return filaDeBloqueados.remove(0);
		else return null;
	}
	
	/*
	 * Realiza o gerenciamento dos processos.
	 * O escalonador inicia o gerenciamento dos processos, somente quando a fila de prontos ou 
	 * a fila de bloqueados n�o estiverem vazias. Gerenciar� os processos quando uma dessas
	 * filas estiverem com um ou mais processos. 
	 * 
	 * Situa��es adversas para o gerenciamento das filas:
	 *	- 	Quando h� somente processos na fila de bloqueados: o tempo de espera de todos os processos
	 *		na fila de bloqueados ser�o decrementados, at� que a fila fique vazia. 
	 *	-	Quando s� existem processos na fila de prontos e TODOS est�o com zero de cr�ditos, ent�o
	 *		o cr�dito de todos os processos ser�o redistribu�dos (restaurados ao original).
	 *	-	Quando houver processos na fila de bloqueados, a cada determinada quantidade de quantum
	 *		os cr�ditos de todos os processos bloqueados precisar�o ser decrementados. 
	 */
	public void inicializar(Despachante d, List<BlocoDeControleDeProcesso> tp) throws FileNotFoundException {
		
		int contador;						// Controla o quantum de cada processo.
		int espera = 0;						// Conta quantos quantums ocorreram.
		int retorno;						// Resultado da execu��o de uma instru��o do programa.
		Processo p;
		boolean continuar;					// Controla quando um processo poder� voltar para fila de prontos.
		boolean bloquear;
		BlocoDeControleDeProcesso bcp;
		double mediaTrocas = 0;
		double mediaInstrucoes = 0;
		double mediaEspera = 0;
		int numProcessos = 0;
		int numQuantums = 0;
		
		while (filaDeProntos.size() != 0 || filaDeBloqueados.size() != 0) {	
					
			 /* Quando s� h� processos na fila de bloqueados. */
			if (filaDeProntos.size() == 0) {
				
				while (filaDeBloqueados.size() != 0) {
					
					decrementarBloqueados();
				}
			}
			
			contador = this.quantum;
			p = removerFilaProntos();
			System.out.println("Executando processo " + p.getNomePrograma());
			p.setEstado("executando");
			if (p.getCreditos() > 0) p.decrementarCreditos();
			continuar = true;
			bloquear = false;
			bcp = buscarBcp(p, tp);
			
			/* Quando s� existe processos com zero cr�ditos e est�o somente na fila de prontos, redistribui os cr�ditos. */
			if (p.getCreditos() == 0 && filaDeBloqueados.size() == 0) {
				
				redistribuirCreditos();
			}
			
			/* Executa o quantum de um processo. */
			while (contador != 0) {
			
				retorno = d.executarProcesso(bcp);
				++mediaInstrucoes;
				--contador;

				if (retorno == 1) {
					
					System.out.println("E/S iniciada em " + p.getNomePrograma());
					p.setEstado("bloqueado");
					p.setTempoEspera(tempoEspera);
					d.salvarContexto(bcp);
					bloquear = true;
					continuar = false;
					break;
				}
				else if (retorno == -1){
					
					System.out.println(p.getNomePrograma() + " terminado. X=" + bcp.getX() + " Y=" + bcp.getY());
					++numProcessos;
					removerBcp(p, tp);
					continuar = false;
					break;	
				}
				else {
					d.continuarContexto(bcp);
					continue;
				}
			}
			
			++mediaTrocas;
			++numQuantums;
			++espera;			// Conta quantos quantums j� ocorreram.
			
			System.out.println("Interrompendo processo " + p.getNomePrograma() + " ap�s " + quantum + " instru��es");
			
			/* Decrementa o tempo de espera de todos os processos bloqueados a cada determinada quantidade de quantums. */
			if (espera == tempoEspera) {
				
				if (filaDeBloqueados.size() != 0 && espera == tempoEspera) {
					
					decrementarBloqueados();
				}
				espera = 0;
			}
			
			/* Adiciona o processo bloqueado na fila */
			if (bloquear == true) adicionarFilaBloqueados(p);
			
			/* Se for o caso, adiciona o processo novamente na fila de prontos. Quando o quantum dele acaba. */
			if (continuar == true) {
				
				p.setEstado("pronto");
				bcp.setEstado("pronto");				
				adicionarFilaProntos(p);
			}
		}
		
		mediaInstrucoes = mediaInstrucoes/numQuantums;
		mediaTrocas = mediaTrocas/numProcessos;
		System.out.println("M�dia de trocas: " + mediaTrocas);
		System.out.println("M�dia de instru��es: " + mediaInstrucoes);
		System.out.println("Quantum: " + quantum);

	}
	
	/*
	 * Remove um processo da tabela de processos do SO.
	 */
	private void removerBcp(Processo p, List<BlocoDeControleDeProcesso> tp) {
		
		for (int i = 0; i < tp.size(); i++) {
		
			if (tp.get(i).getProcesso() == p) tp.remove(i);
		}	
	}
	
	/*
	 * Busca o BCP de um processo na tabela de processos do SO.
	 */
	private BlocoDeControleDeProcesso buscarBcp(Processo p, List<BlocoDeControleDeProcesso> tp) {
		
		for (BlocoDeControleDeProcesso i : tp) {
			
			if (i.getProcesso() == p) return i;
		}
		
		return null;
	}
	
	/*
	 * Decrementa o tempo de espera de todos os processos na fila de bloqueados.
	 * S� decrementa o tempo se for maior que zero.
	 * Quando um processo possui zero de tempo, � adicionado na fila de prontos.
	 */
	private void decrementarBloqueados() {
		
		for (int i = 0; i < filaDeBloqueados.size(); i++) {
			
			if (filaDeBloqueados.get(i).getTempoEspera() > 0) filaDeBloqueados.get(i).decrementarTempoEspera();
			else adicionarFilaProntos(filaDeBloqueados.remove(i));
		}
	}
	
	/*
	 * Registribui os cr�ditos de todos os processos da fila de prontos.
	 * M�todo chamado quando s� existem processos na fila de prontos e 
	 * todos eles est�o com zero de cr�ditos.
	 */
	private void redistribuirCreditos() {
		
		for (Processo i : filaDeProntos) {
			
			i.setCreditos(i.getPrioridade());
		}
	}
	
	
	public void imprimirFila() {
		
		System.out.println(filaDeProntos);
	}
	
	public int getQuantum() {
		
		return this.quantum;
	}
}
